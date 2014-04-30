import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class Main
{

    public static void main(String... args)
    {
        String solr = "http://localhost:8983/solr/";
        SolrServer server = new HttpSolrServer(solr);
        try {
            server.deleteByQuery( "*:*" );
        } catch (SolrServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            String baseUrl = "http://www.stepstone.de";
            Document doc = Jsoup.connect(baseUrl)
                                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                                    .referrer("http://www.google.com")
                                    .get();

            Elements jobFields = doc.select("#hotJobs").select("a");
            for(Element jobField : jobFields)
            {
                String link = jobField.attr("href");
                String fieldName = jobField.text();

                try {
                    Document fieldDoc = Jsoup.connect(baseUrl + link)
                                                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                                                .referrer("http://www.google.com")
                                                .get();



                    Elements fieldCats = fieldDoc.select("#MostPopularJobTitles").select(".column.left").select(".subrow");
                    fieldCats.remove(0);
                    for(Element fieldCat : fieldCats)
                    {
                        String value = fieldCat.select("input").attr("value");
                        String fieldCatName = fieldCat.select("label").text();
                        String fieldCatLink = "http://www.stepstone.de/5/ergebnisliste.html?offset=0&function=" + value;

                        try {
                            Document fieldCatDoc = Jsoup.connect(fieldCatLink)
                                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                                    .referrer("http://www.google.com")
                                    .get();

                            Elements jobs = fieldCatDoc.select(".job_info").select("a");
                            for(Element job : jobs)
                            {
                                String jobLink = job.attr("href");

                                try {
                                    Document jobDoc = Jsoup.connect(baseUrl + jobLink)
                                            .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                                            .referrer("http://www.google.com")
                                            .get();

                                    String data = jobDoc.select("#PageContent").text();
//                                    data = StringEscapeUtils.unescapeHtml4(data);

                                    String[] plainTokens = data.split(" ");
                                    List<String> tokens = new ArrayList<String>();


                                    List<String> blacklist = new ArrayList<String>();
                                    BufferedReader br = null;
                                    String sCurrentLine;
                                    br = new BufferedReader(new FileReader("C:\\Users\\sven.kreiter.QMEDIA\\Desktop\\Projekte\\kreditplattform\\JobMatcher\\src\\blacklist.txt"));
                                    while ((sCurrentLine = br.readLine()) != null) {
                                        blacklist.add(sCurrentLine);
                                    }

                                    Collection<SolrInputDocument> solrDocs = new ArrayList<SolrInputDocument>();
                                    int id = 0;
                                    for(String plainToken : plainTokens)
                                    {
                                        if(plainToken.length() > 2 && !blacklist.contains(plainToken.toLowerCase()))
                                        {

                                            SolrInputDocument solrDoc = new SolrInputDocument();
                                            solrDoc.addField( "id", id);
                                            solrDoc.addField( "title", fieldName, 1.0f );
                                            solrDoc.addField( "subject", fieldCatName, 1.0f );
                                            solrDoc.addField("description", plainToken, 1.0f);

                                            solrDocs.add(solrDoc);
                                            id++;
                                        }
                                    }

                                    server.add(solrDocs);
                                    server.commit();

                                    break;

                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (SolrServerException e) {
                                    e.printStackTrace();
                                }
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        break;
                    }

                    break;

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
