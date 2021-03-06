/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.tdbloader4;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.jena.tdbloader4.io.LongQuadWritable;
import org.apache.jena.tdbloader4.partitioners.InputSampler;
import org.apache.jena.tdbloader4.partitioners.Sampler;
import org.apache.jena.tdbloader4.partitioners.SplitSampler;
import org.apache.jena.tdbloader4.partitioners.TotalOrderPartitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FourthDriver extends Configured implements Tool {

    private static final Logger log = LoggerFactory.getLogger(FourthDriver.class);

    public FourthDriver () {
		super();
        log.debug("constructed with no configuration.");
	}

	public FourthDriver (Configuration configuration) {
		super(configuration);
        log.debug("constructed with configuration.");
	}
    
	@Override
	public int run(String[] args) throws Exception {
		if ( args.length != 2 ) {
			System.err.printf("Usage: %s [generic options] <input> <output>\n", getClass().getName());
			ToolRunner.printGenericCommandUsage(System.err);
			return -1;
		}

		Configuration configuration = getConf();
        int num_samples = configuration.getInt(Constants.OPTION_NUM_SAMPLES, Constants.OPTION_NUM_SAMPLES_DEFAULT);
        int max_splits_sampled = configuration.getInt(Constants.OPTION_MAX_SPLITS_SAMPLED, Constants.OPTION_MAX_SPLITS_SAMPLED_DEFAULT);

        // We need to call setPartitionFile before we create a new Job!
        FileSystem fs = FileSystem.get(configuration);
        TotalOrderPartitioner.setPartitionFile(configuration, new Path(args[0], "_partitions").makeQualified(fs));

		Job job = new Job(configuration);
		job.setJobName(Constants.NAME_FOURTH);
		job.setJarByClass(getClass());

		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		job.setInputFormatClass(SequenceFileInputFormat.class);

		job.setMapperClass(FourthMapper.class);
		job.setMapOutputKeyClass(LongQuadWritable.class);
		job.setMapOutputValueClass(NullWritable.class);

		job.setReducerClass(FourthReducer.class);
		job.setOutputKeyClass(LongQuadWritable.class);
		job.setOutputValueClass(NullWritable.class);

	    Utils.setReducers(job, configuration, log);

       	if ( log.isDebugEnabled() ) Utils.log(job, log);

        log.debug("Running input sampler...");
        Sampler<LongQuadWritable, NullWritable> sampler = new SplitSampler<LongQuadWritable, NullWritable>(num_samples, max_splits_sampled);
        InputSampler.writePartitionFile(job, sampler);
        log.debug("Input sampler finished.");
       	
		return job.waitForCompletion(true) ? 0 : 1;
	}
	
	public static void main(String[] args) throws Exception {
        log.debug("main method: {}", Utils.toString(args));
	    int exitCode = ToolRunner.run(new FourthDriver(), args);
		System.exit(exitCode);
	}

}
