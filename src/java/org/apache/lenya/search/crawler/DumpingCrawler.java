/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

/* $Id: ContentHandler.java 473841 2006-11-12 00:46:38Z gregor $  */

package org.apache.lenya.search.crawler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import websphinx.Crawler;
import websphinx.EventLog;
import websphinx.Link;
import websphinx.LinkTransformer;
import websphinx.Page;

/**
 * Crawler which creates a dump of a website.
 */
public class DumpingCrawler extends Crawler {

    private String crawlScopeURL;

    private File dumpDir;
    
    /**
     * Specify types of links which should be followed.
     * @see websphinx.StandardClassifier
     */
    private static final String[] LINK_TYPES = {"hyperlink", "image", "code", "header-link"};

    /**
     * Creates a new SimpleCrawler.
     * @param crawlStartURL 
     *          The URL where the crawl should start. Has to include a filename
     *          and extension, e.g. http://wyona.org/index.html
     * @param crawlScopeURL
     *          Limits the scope of the crawl, only links which match the scope url  
     *          will be followed.
     *          In most cases, the crawlScopeURL is the parent of the crawlStartURL.
     * @param dumpDir  
     *          The directory in the filesystem where the dumped files will be stored.
     *          Does not have to exist yet, it will be created by the crawler.
     */
    public DumpingCrawler(String crawlStartURL, String crawlScopeURL, File dumpDir) {
        try {
            this.setRoot(new Link(crawlStartURL));
        } catch (MalformedURLException e) {
            this.setRoot(null);
        }
        if (!crawlStartURL.startsWith(crawlScopeURL)) {
            throw new IllegalArgumentException("crawlScopeURL [" + crawlScopeURL + 
                    "] must be a prefix of crawlStartURL [" + crawlStartURL + "]");
        }
        if (dumpDir.exists() && !dumpDir.isDirectory()) {
            throw new IllegalArgumentException("dumpDir [" + dumpDir.getAbsolutePath() + 
                    "] is not a directory.");
        }
        this.crawlScopeURL = crawlScopeURL;
        this.dumpDir = dumpDir;
        this.setSynchronous(true);
        this.setDomain(Crawler.SERVER);
        this.setLinkType(LINK_TYPES);
    }

    /**
     * @see websphinx.Crawler#visit(websphinx.Page)
     */
    public void visit(Page page) {
        String pageURL = page.getURL().toString();
        if (pageURL.startsWith(this.crawlScopeURL)) {
            File file = new File(this.dumpDir, pageURL.substring(this.crawlScopeURL.length()));
            //System.out.println("writing file: " + file.getAbsolutePath());
            try {
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                LinkTransformer linkTransformer = new LinkTransformer(new FileOutputStream(file));
                linkTransformer.setBase(page.getBase());
                linkTransformer.writePage(page);
                linkTransformer.flush();
            } catch (IOException e) {
                throw new RuntimeException("Could not save page: url=" + page.getURL() + ", file=" 
                        + file + ": " + e, e);
            }
        }
    }

    public static void main(String[] args) {
        String crawlStartURL = args[0];
        String crawlScopeURL = args[1];
        String dumpDir = args[2];
        
        DumpingCrawler crawler = new DumpingCrawler(crawlStartURL, crawlScopeURL, new File(dumpDir));
        
        EventLog eventLog = new EventLog(System.out);
        crawler.addCrawlListener(eventLog);
        crawler.addLinkListener(eventLog);
        
        crawler.run();
    }
}
