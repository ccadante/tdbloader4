/*
 * Copyright 2010,2011 Talis Systems Ltd.
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

package com.talis.labs.tdb.tdbloader3;

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.sparql.core.Quad;
import com.talis.labs.tdb.tdbloader3.io.QuadWritable;

public class FirstMapper extends Mapper<LongWritable, QuadWritable, Text, NullWritable> {

    private static final Logger log = LoggerFactory.getLogger(FirstMapper.class);

    private Text st = new Text();
    private Text pt = new Text();
    private Text ot = new Text();
    private Text gt = new Text();
    private NullWritable outputValue = NullWritable.get();

    @Override
    public void map (LongWritable key, QuadWritable value, Context context) throws IOException, InterruptedException {
        if ( log.isDebugEnabled() ) log.debug("< ({}, {})", key, value);
        Quad quad = value.getQuad();
        String s = Utils.serialize(quad.getSubject());
        String p = Utils.serialize(quad.getPredicate());
        String o = Utils.serialize(quad.getObject());
        String g = null;
        if ( !quad.isDefaultGraphGenerated() ) {
            g = Utils.serialize(quad.getGraph());
        }

        try {
            if ( ( s != null ) && ( p != null ) && ( o != null) ) {
                st.set(s);
                pt.set(p);
                ot.set(o);
                
                emit(context, st);
                emit(context, pt);
                emit(context, ot);
            }
            if ( g != null ) {
                gt.set(g);
                emit(context, gt);
            }
        } catch (Exception e) {
            throw new TDBLoader3Exception(e);
        } finally {
            st.clear();
            pt.clear();
            ot.clear();
            gt.clear();
        }
    }

    private void emit ( Context context, Text key ) throws IOException, InterruptedException {
        context.write(key, outputValue);
        if ( log.isDebugEnabled() ) {
            log.debug("> ({}, {})", key, outputValue);
        }
    }

}
