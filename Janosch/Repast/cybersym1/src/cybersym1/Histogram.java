package cybersym1;

import java.awt.Color;
import java.util.Hashtable;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RefineryUtilities;

import repast.simphony.engine.schedule.ScheduledMethod;

/**
 * <h1>Histogram</h1>
 * 
 * Custom bar chart that displays the system's Resource demands, Resource inventory, Resource 
 * requests and the number of exhausted Sources per Resource type (letter). Uses the jFreeChart 
 * class.
 * 
 * @author Janosch Haber, 10400192, University of Amsterdam (UvA)
 * @date 06-05-2015
 */
public class Histogram extends JFrame {
	
	// The bar chart's features
	private static final long serialVersionUID = 1L;
	private JFrame frame = new JFrame();
	private JFreeChart chart;
	private ChartPanel chartPanel = new ChartPanel(chart);

	/**
	 * Histogram Constructor. Creates an initial instance of the Resource Statistics bar chart and 
	 * initializes the display. At this stage, only the Agent demands will be ready for display.
	 */
	public Histogram(){
		super("Resource Statistics");
		chart = createChart(getDataset());
		ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBackground(Color.white);
        chartPanel.setPreferredSize(new java.awt.Dimension(750, 500));
        frame.setContentPane(chartPanel);
        frame.pack();
        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
    }
   
	/**
	 * Scheduled Method for updating the Resource Statistics display for every tick
	 * TODO: This should be invoked last for every iteration 
	 */
	@ScheduledMethod(start=1, interval=1)
	public void refreshHistogram() {
		Histogram.this.updateChart();
	}

	/**
	 * Generates a categorized dataset for the Resource Statistics' bar chart display
	 * @return a {@code CategoryDataset} of Resource demands, Resouce inventory, Resource 
	 * requests and the number of exhausted Sources per Resource type (letter)
	 */
    public CategoryDataset getDataset() {
    	DefaultCategoryDataset dataset = new DefaultCategoryDataset( );  
 	   	for(char letter = 'A'; letter <= 'Z';letter++) {
 		    String category = String.valueOf(letter);
 		    Integer demand = CybersymBuilder.letterDemandRegister.get(letter);
 		    if (demand == null) demand = 0; 
 		    //System.out.println("Demand for " + category + " is " + demand);
 		    dataset.addValue((double)demand, "Demand", category);
 		    Integer supply = CybersymBuilder.letterSupplyRegister.get(letter);
 		    if (supply == null) supply = 0; 
 		    //System.out.println("Supply for " + category + " is " + supply);
 		    dataset.addValue((double)supply, "Supply", category);
 		    Integer request = CybersymBuilder.letterRequestRegister.get(letter);
 		    if (request == null) request = 0; 
 		    //System.out.println("Request for " + category + " is " + request);
 		    dataset.addValue((double)request, "Request", category);
 		    Integer depletion = CybersymBuilder.letterDepletionRegister.get(letter);
 		    if (depletion == null) depletion = 0; 
 		    //System.out.println("Depletion for " + category + " is " + depletion);
 		    dataset.addValue((double)depletion, "Depletion", category);
 	   }
       return dataset;            
    }
   
    /**
     * Updates the Resource Statistics bar chart visualization
     */
    public void updateChart(){
    	this.chart = createChart(getDataset());
        this.chart.fireChartChanged();
        this.chartPanel.setChart(this.chart);
        this.chartPanel.updateUI();
        this.frame.setContentPane(this.chartPanel);
        
        // Refresh the Resource request register. Resource requests are generated again for every
        // iteration, so counts would increase undesignedly
        CybersymBuilder.letterRequestRegister = new Hashtable<Character,Integer>();
    }   

    /**
     * Generates the content of the Resource Statistics bar chart
     * @param dataset A {@code CategoryDataset} of Resource demands, Resouce inventory, Resource 
	 * requests and the number of exhausted Sources per Resource type (letter)
     * @return A readily set {@code JFreeChart} for display
     */
    private JFreeChart createChart(final CategoryDataset dataset) {
       final JFreeChart chart = ChartFactory.createBarChart(
    	         "Resource Statistics",           
    	         "Resource Type",            
    	         "Amount",            
    	         getDataset(),          
    	         PlotOrientation.VERTICAL,           
    	         true, true, false);

        chart.setBackgroundPaint(Color.white);
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setRangeGridlinePaint(Color.lightGray);
        return chart;
    }
}