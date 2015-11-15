import java.util.*;
import java.lang.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.Connection;

/*
 * The node class is the functional component of the crawler. Once a 
 * node is created, and crawl is called on it, it recursively calls its
 * getLinks() method on all links it finds at its address. Should only create
 * a new node if the address has not been seen before. 
 * hasBeenVisited field prevents crawler from crawling in an infinite loop
 *
 */
public class Node
{
    /*
     * Hashmaps containing nodes which this node is linked to, or 
     * other nodes which have links to this node/
     */
    private HashMap<String,Node> connectedToMe;
    private HashMap<String,Node> isConnectedTo;
    private WebCrawl crawler;
    private boolean hasBeenVisited;

    private String domain;
    private String address;

    public Node(String address, String domain, WebCrawl crawler)
    {
        connectedToMe = new HashMap<String,Node>();
        isConnectedTo = new HashMap<String,Node>();

        /* 
         * These lines get the domain name on its own, i.e. 
         * gocardless.com instead of https://developers.gocardless.com
         */
        if(domain.length() - domain.replace(".", "").length() >= 2)
            this.domain = domain.replaceAll("^((http|https)://([a-z0-9-]*\\.)?)","").replaceAll("/[a-z0-9-/]*","");
        else
            this.domain = domain.replaceAll("^((http|https)://)","").replaceAll("/[a-z0-9-/]*","");
        if(!domain.matches("/$"))
            domain += "/";

        this.address  = address;
        this.crawler  = crawler;
        this.hasBeenVisited = false;
    }

    /* 
     * Gets links from domain using JSoup HTML parser to extract links.
     * Checks that the link is not to an external domain, and that it 
     * has not already been recorded. Allows https versions of the current
     * domain.
     */
    private void getLinks() 
    {
        try 
        {
            Connection.Response con = Jsoup.connect(this.address)
                                            .ignoreContentType(true)
                                            .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")  
                                            .referrer("http://www.google.com")   
                                            .timeout(12000) 
                                            .followRedirects(true)
                                            .execute();

            if(con.statusCode() == 200)
            {
                Document doc = con.parse();

                this.address = doc.location();
                ArrayList<Element> links = doc.getElementsByAttribute("href");

                for(Element t: links)
                {
                    String a = t.attr("abs:href").toString().trim();
                    /*
                     * Attempts to remove args from address
                     */
                    if(a.contains("#"))
                            a = a.substring(0,a.indexOf('#'));               
                    if(a.contains("?"))
                            a = a.substring(0,a.indexOf('?'));               

                    if(a.length() != 0)
                    {
                        if(a.matches("^((http|https)://([a-z0-9-]*\\.)?" + this.domain + ").*"))
                        {
                            this.addNode(new Node(a,this.domain,this.crawler));
                        }
                    }
                }
                crawler.drawNode(this);
            }
            else
            {
                this.isConnectedTo.clear();
                for(Node t:connectedToMe.values())
                    t.isConnectedTo.remove(this.getAddress());
                this.connectedToMe.clear();
            }
        }
        catch(Exception E) 
        {
        }
    }

    /* 
     * Checks if link has been inserted already, and then adds the new node to the
     * connected nodes list. Otherwise finds the correct node and adds the connection.
     * This is necessary so that we do not create 2 nodes representing
     * the same page.
     * nb. getLinks() always creates a new node, whether or not it has been seen before, 
     * but all references to it will be removed if it has, and (hopefully) be collected
     * by our trusty garbage collector
     */
    private void addNode(Node link)
    {
        if(!this.crawler.hasLinkBeenInserted(link))
        {
            System.out.println(link.getAddress());
            link.addConnection(this);
            this.connectToNode(link);
            this.crawler.insertLink(link);
        }
        else //i.e. add a link from this node to the already existing node
        {
            Node node = crawler.getNode(link.getAddress());
            if(this != node)
            {
                node.addConnection(this);
                this.connectToNode(node);
            }
        }
    }

    public HashMap<String,Node> getConnectedNodes()
    {
        return this.connectedToMe;
    }

    /*
     * Recursively calls crawl on nodes connected to this node
     */
    public void crawl()
    {
        this.getLinks();
        this.hasBeenVisited = true;
        for(Node t:isConnectedTo.values())
        {
            if(!t.getHasBeenVisited())
                t.crawl();
        }
    }

    public void setHasBeenVisited(boolean b)
    {
        this.hasBeenVisited = b;
    }

    public boolean getHasBeenVisited()
    {
        return this.hasBeenVisited;
    }

    public String getAddress()
    {
        return this.address;
    }

    public Node connectToNode(Node connection)
    {
        this.isConnectedTo.put(connection.getAddress(),connection);
        return connection;
    }

    public Node addConnection(Node connection)
    {
        this.connectedToMe.put(connection.getAddress(),connection);
        return connection;
    }
}
