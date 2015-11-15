import javax.swing.JFrame;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

/* 
 * This class instantiates the first node, necessary to begin crawling. 
 * It also keeps a hashmap with references to all addresses which have been
 * indexed. This is what the Node class uses to find out whether or not an
 * address has been indexed or not.
 */
public class WebCrawl
{
    private HashMap<String,Node> linksIndexed;
    private Node baseNode;
    private Viewer view;

    public WebCrawl(String domain)
    {
        this.linksIndexed  = new HashMap<String,Node>();
        try
        {
            Document doc = Jsoup.connect(domain).get();
            domain = doc.location();
        } catch(Exception e) {
        }

        view = new Viewer(this);

        view.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        view.setSize(1028, 768);
        view.setVisible(true);

        Thread thread = new Thread(view);
        thread.start();

        this.baseNode = new Node(domain,domain,this);
        this.drawNode(this.baseNode);
        this.insertLink(this.baseNode);
        this.baseNode.setHasBeenVisited(true);
    }

    public void crawl()
    {
        this.baseNode.crawl();
        System.out.println("Finished crawling!");
    }

    /* 
     * These 2 methods record and find which links have been inserted. hasLinkBeenInserted()
     * removes http/https and www. parts of the string, as these will usually result in a link
     * to the same page
     */
    public void insertLink(Node link)
    {
        this.linksIndexed.put(link.getAddress().replaceAll("(http|https)://(www\\.)?",""),link);
    }

    public boolean hasLinkBeenInserted(Node link)
    {
        return this.linksIndexed.containsKey(link.getAddress()
                                             .replaceAll("(http|https)://(www\\.)?",""));
        
    }
    
    /*
     * Used to create connections between Nodes once it has been realised that 
     * a node already exists. Used to get the correct node to add a connection
     * to
     */
    public Node getNode(String address)
    {
        return linksIndexed.get(address.replaceAll("(http|https)://",""));
    }

    /*
     * This is called by Nodes in order to add themselves to view's drawQueue
     */
    public void drawNode(Node node)
    {
        this.view.addNodeToDraw(node);
    }

    public static void main(String[] args)
    {
        if(args.length > 0)
        {
            if(!args[0].matches("http"))
                    args[0] = "http://" + args[0];
            WebCrawl crawler = new WebCrawl(args[0]);
            crawler.crawl();
        }
        else
            System.out.println("Not enough arguments: web address should be supplied as argument");
    }
}
