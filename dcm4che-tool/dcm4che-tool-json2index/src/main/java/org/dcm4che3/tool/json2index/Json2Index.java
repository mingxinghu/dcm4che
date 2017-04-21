/*
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * J4Care.
 * Portions created by the Initial Developer are Copyright (C) 2015-2017
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 */

package org.dcm4che3.tool.json2index;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.stream.JsonGenerator;
import java.io.*;
import java.util.*;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @since April 2017
 */
public class Json2Index {

    private final File indir;
    private final Map<String,ParentRef> parents = new HashMap<>();

    public Json2Index(File indir) {
        this.indir = indir;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: json2rst <schema-dir>");
            System.exit(-1);
        }
        Json2Index json2Rst = new Json2Index(new File(args[0]));
        json2Rst.process();
    }

    private void process() throws IOException {
        try (JsonGenerator gen = Json.createGenerator(System.out)) {
            gen.writeStartObject();
            gen.writeStartObject("schemas");
            for (File file : indir.listFiles()) {
                gen.writeStartObject(file.getName());
                processFile(file, gen);
                gen.writeEnd();
            }
            gen.writeEnd();
            gen.writeStartObject("parents");
            for (Map.Entry<String, ParentRef> entry : parents.entrySet()) {
                gen.writeStartObject(entry.getKey());
                ParentRef ref = entry.getValue();
                gen.writeStartArray(ref.property);
                for (String schema : ref.schemas) {
                    gen.write(schema);
                }
                gen.writeEnd();
                gen.writeEnd();
            }
            gen.writeEnd();
            gen.writeEnd();
        }
    }

    private void processFile(File inFile, JsonGenerator gen) throws IOException {
        try (InputStreamReader is = new InputStreamReader(new FileInputStream(inFile))) {
            JsonReader reader = Json.createReader(is);
            JsonObject doc = reader.readObject();
            gen.write("title", doc.getString("title"));
            gen.write("description", doc.getString("description"));
            gen.writeStartObject("properties");
            JsonObject properties = doc.getJsonObject("properties");
            for (String name : properties.keySet()) {
                gen.writeStartArray(name);
                JsonObject property = properties.getJsonObject(name);
                gen.write(property.getString("title"));
                gen.write(property.getString("description"));
                updateParents(inFile, name, property);
                gen.writeEnd();
            }
            gen.writeEnd();
        }
    }

    private void updateParents(File inFile, String name, JsonObject property) {
        JsonObject items = property.getJsonObject("items");
        JsonObject typeObj = items == null ? property : items;
        JsonString refObj = typeObj.getJsonString("$ref");
        if (refObj != null) {
            String ref = refObj.getString();
            ParentRef parentRefs = parents.get(ref);
            if (parentRefs == null) {
                parents.put(ref, parentRefs = new ParentRef(name));
            }
            parentRefs.schemas.add(inFile.getName());
        }
    }

    private class ParentRef {
        String property;
        List<String> schemas = new ArrayList<>(2);

        public ParentRef(String property) {
            this.property = property;
        }
    }
}
