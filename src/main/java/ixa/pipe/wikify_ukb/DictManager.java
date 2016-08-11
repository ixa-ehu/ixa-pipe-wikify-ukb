/*
 * Copyright (C) 2016 IXA Taldea, University of the Basque Country UPV/EHU

   This file is part of ixa-pipe-wikify-ukb.

   ixa-pipe-wikify-ukb is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   ixa-pipe-wikify-ukb is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with ixa-pipe-wikify-ukb.  If not, see <http://www.gnu.org/licenses/>.

*/

package ixa.pipe.wikify_ukb;

import java.io.File;
import java.util.Map;
import org.mapdb.DB;
import org.mapdb.DBMaker;


public class DictManager{
    private DB db;
    private Map<String, String> map;
    
    
    public DictManager(String dbName,String hashName){
	File file = new File(dbName);
	db = DBMaker.newFileDB(file).readOnly().closeOnJvmShutdown().make();
	map = db.getHashMap(hashName);
    }

    public String getValue(String id){
	return map.get(id);
    }
}