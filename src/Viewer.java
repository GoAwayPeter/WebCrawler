import javax.swing.JFrame;
import java.util.*;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import com.mxgraph.layout.*;

/*
 * This class runs in a seperate thread to the rest of the 
 * web crawler, and is used to display a network of the currently
 * scanned website. At the moment, the automatic laying out of the
 * network leaves something to be desired, but this could be 
 * worked on later, if necessary.
 */
public class Viewer extends JFrame implements Runnable
{
    private WebCrawl crawler;
    private mxGraph graph;
    private Object parent;
    private Object base;
    private mxOrganicLayout layout;

    /*
     * Hashmaps containing the nodes which have been drawn, in order
     * to keep track of the links between them, and when the links can
     * be drawn. DrawnObjects keeps references to the actual object that 
     * represents the node in the interface, and can be manipulated 
     * via its hashmap.
     *
     * drawQueue is where all the nodes are drawn from, Nodes add themselves
     * to this queue once they have determined that they contain a valid URL.
     * May revise this to only draw nodes which contain links in order to 
     * avoid drawing images, css, pdf files etc.
     */
    private HashMap<String,Node> nodesDrawn;
    private HashMap<String,Object> drawnObjects;
    private LinkedList<Node> drawQueue;

    /*
     * Takes a reference to the crawler so that nodes can communicate 
     * with the addNodeToQueue() method.
     */
    public Viewer(WebCrawl crawler)
    {
		super("Site Map Viewer");
    
        this.crawler = crawler;
		this.graph = new mxGraph();
		this.parent = this.graph.getDefaultParent();
        this.layout = new mxOrganicLayout(this.graph);
        this.drawQueue = new LinkedList<Node>();
        /*
         * Experimenting with layout options
        this.layout.setMaxDistanceLimit(0);   
        this.layout.setFineTuningRadius(2000);
        this.layout.setMinDistanceLimit(0);
        */
        this.nodesDrawn = new HashMap<String,Node>();
        this.drawnObjects = new HashMap<String,Object>();

		mxGraphComponent graphComponent = new mxGraphComponent(this.graph);
		getContentPane().add(graphComponent);
    }

    /*
     * Gets nodes from the queue, checks if they have already been drawn, 
     * and then draws them if not. Finds nodes which are connected to the
     * current node, and draws connections between them
     */
    public void run()
    {
        while(true)
        {
            this.graph.getModel().beginUpdate();
            try
            {
                /* 
                 * Draws Nodes
                 */
                Node t = null;
                try
                {
                    t = drawQueue.remove();
                } catch(Exception e)
                {
                }
                if(t != null)
                {
                    if(!this.nodesDrawn.containsKey(t.getAddress()))
                    {
                        this.nodesDrawn.put(t.getAddress(),t);
                        Object obj = this.graph.insertVertex(this.parent,null,
                                                   t.getAddress(),512,384,1,1,null,false); 
                                                    
                        this.drawnObjects.put(t.getAddress(),obj);

                        for(Node node:t.getConnectedNodes().values())
                        {
                            if(node != null && obj != null)
                            {
                                if(this.drawnObjects.get(node.getAddress()) != null)
                                    this.graph.insertEdge(this.parent,null,"",this.drawnObjects
                                                                          .get(node.getAddress()),
                                                                           obj);
                            }
                        }
                    }
                }

                if(this.parent != null)
                    layout.execute(this.parent);
            }
            finally
            {
                this.graph.getModel().endUpdate();
            }
        }
    }

    public void addNodeToDraw(Node node)
    {
        this.drawQueue.add(node);
    }
}
