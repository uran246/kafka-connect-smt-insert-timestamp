
/*
 * Copyright © 2019 Christopher Matta (chris.matta@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.itcur.smt;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class InsertTimestampTest {

    private final InsertTimestamp<SourceRecord> xform = new InsertTimestamp.Value<>();

    @After
    public void tearDown() throws Exception {
        xform.close();
    }

    @Test(expected = DataException.class)
    public void topLevelStructRequired() {
        xform.configure(Collections.singletonMap("timestamp.field.name", "myTimestamp"));
        xform.apply(new SourceRecord(null, null, "", 0, Schema.INT64_SCHEMA, 42L));
    }

    @Test
    public void copySchemaAndInsertTimestampField() {
        final Map<String, Object> props = new HashMap<>();

        props.put("timestamp.field.name", "myTimestamp");

        xform.configure(props);

        final Schema simpleStructSchema = SchemaBuilder.struct().name("name").version(1).doc("doc").field("magic", Schema.OPTIONAL_INT64_SCHEMA).build();
        final Struct simpleStruct = new Struct(simpleStructSchema).put("magic", 42L);

        final SourceRecord record = new SourceRecord(null, null, "test", 0, simpleStructSchema, simpleStruct);
        final SourceRecord transformedRecord = xform.apply(record);

        assertEquals(simpleStructSchema.name(), transformedRecord.valueSchema().name());
        assertEquals(simpleStructSchema.version(), transformedRecord.valueSchema().version());
        assertEquals(simpleStructSchema.doc(), transformedRecord.valueSchema().doc());

        assertEquals(Schema.OPTIONAL_INT64_SCHEMA, transformedRecord.valueSchema().field("magic").schema());
        assertEquals(42L, ((Struct) transformedRecord.value()).getInt64("magic").longValue());
        assertEquals(Schema.INT64_SCHEMA, transformedRecord.valueSchema().field("myTimestamp").schema());
        assertNotNull(((Struct) transformedRecord.value()).getInt64("myTimestamp"));

        // Exercise caching
        final SourceRecord transformedRecord2 = xform.apply(
                new SourceRecord(null, null, "test", 1, simpleStructSchema, new Struct(simpleStructSchema)));
        assertSame(transformedRecord.valueSchema(), transformedRecord2.valueSchema());

    }

    @Test
    public void schemalessInsertTimestampField() {
        final Map<String, Object> props = new HashMap<>();

        props.put("timestamp.field.name", "myTimestamp");

        xform.configure(props);

        final SourceRecord record = new SourceRecord(null, null, "test", 0,
                null, Collections.singletonMap("magic", 42L));

        final SourceRecord transformedRecord = xform.apply(record);
        assertEquals(42L, ((Map) transformedRecord.value()).get("magic"));
        assertNotNull(((Map) transformedRecord.value()).get("myTimestamp"));

    }
}