/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

package grant.wu.trading.ui;

import grant.wu.trading.TestTradingDynamicPlacement;

import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;

import javax.swing.BorderFactory; 
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JLabel;
import javax.swing.JPanel; 
import javax.swing.JFrame;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

/*
 * BorderDemo.java requires the following file:
 *    images/wavy.gif
 */
public class TradingTestUI extends JPanel {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TradingTestUI() {
        super();
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        //Keep references to the next few borders,
        //for use in titles and compound borders.

        //A border that puts 10 extra pixels at the sides and
        //bottom of each pane.
        Border paneEdge = BorderFactory.createEmptyBorder(0,10,10,10);

        //First pane: problem
        JPanel problemPanel = new JPanel();
        problemPanel.setBorder(paneEdge);

        String[] labels = {"ProblemSize", "Dynamic", "DynamicVmStartingTime", "RunTest"};
        String[] descs = {"problem size(INT 1-10, 1 for 50 VMs, 10 for 500 VMs) ", "dynamic VM?(Y/N)", "dynamic VM starting time( INT eg. 200)", "which test to run(F for FFD,S for single market,M for multiple market)"};
        JPanel p = createParameterPanel(labels, descs);
        problemPanel.add(p);
        //Second pane: FFD
        JPanel ffdPanel = new JPanel();
        ffdPanel.setBorder(paneEdge);
        ffdPanel.setLayout(new BoxLayout(ffdPanel,
                                              BoxLayout.Y_AXIS));

        addCompForBorder(BorderFactory.createTitledBorder("FFD parameters"),
                         " area for parameters",
                         ffdPanel);

        //Third pane: titled borders
        JPanel tradingSingleMarketPanel = new JPanel();
        tradingSingleMarketPanel.setBorder(paneEdge);
        String[] tradingLabels = {"NetworkAwareInSingle"};
        String[] tradingDescs = {"network aware in single market?(Y/N)"};
        tradingSingleMarketPanel.add(createParameterPanel(tradingLabels, tradingDescs));

        //Fourth pane: compound borders
        JPanel tradingMultipleMarketPanel = new JPanel();
        tradingMultipleMarketPanel.setBorder(paneEdge);
        String[] tradingMultipleLabels = {"NetworkAwareInMultiple", "MarketNumber"};
        String[] tradingMultipleDescs = {"network aware in multiple market?(Y/N)", "market number(INT currently fixed as 2)"};
        tradingMultipleMarketPanel.add(createParameterPanel(tradingMultipleLabels, tradingMultipleDescs));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Problem", null, problemPanel, null);
        tabbedPane.addTab("FFD", null, ffdPanel, null);
        tabbedPane.addTab("Trading in Single Market", null, tradingSingleMarketPanel, null);
        tabbedPane.addTab("Trading in Multiple Markets", null, tradingMultipleMarketPanel, null);
        tabbedPane.setSelectedIndex(0);
        String toolTip = new String("<html>Blue Wavy Line border art crew:<br>&nbsp;&nbsp;&nbsp;Bill Pauley<br>&nbsp;&nbsp;&nbsp;Cris St. Aubyn<br>&nbsp;&nbsp;&nbsp;Ben Wronsky<br>&nbsp;&nbsp;&nbsp;Nathan Walrath<br>&nbsp;&nbsp;&nbsp;Tommy Adams, special consultant</html>");
        tabbedPane.setToolTipTextAt(1, toolTip);

        add(tabbedPane);
        
        JPanel botPanel = new JPanel();
        JButton btnRun = new JButton("run");
        botPanel.add(btnRun);
        final JTextArea consoleArea = new JTextArea("here to display console");
        consoleArea.setAutoscrolls(true);
        btnRun.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				StringBuilder sb = new StringBuilder();
				for(String param : paramFieldMap.keySet()){
					sb.append(param +": ");
					sb.append(paramFieldMap.get(param).getText());
					sb.append("\r\n");
										
				}				
				consoleArea.setText(sb.toString());
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						btnRun.setEnabled(false);
						runTest(consoleArea);
						btnRun.setEnabled(true);
					}
				}).start();
				
			}
		});
    
        botPanel.setPreferredSize(new Dimension(10, 10));
        add(botPanel);
        JScrollPane scrollPane = new JScrollPane(consoleArea);
        add(scrollPane);
        setPreferredSize(new Dimension(800,600));
    }
	
	protected void runTest(JTextArea consoleArea) {
		
		int problemSize = Integer.parseInt(paramFieldMap.get("ProblemSize").getText());
		if (problemSize<1 || problemSize>10){
			consoleArea.append("incorrect problem size ");
		}
		
		boolean dynamic = paramFieldMap.get("Dynamic").getText().equalsIgnoreCase("Y");
		
		int dynamicVmStartingNumber = Integer.parseInt(paramFieldMap.get("DynamicVmStartingTime").getText());
		if (dynamicVmStartingNumber < 100 || dynamicVmStartingNumber>600 * problemSize){
			consoleArea.append("incorrect dynamicVmStartingTime ");
		}
		
		String runTest = paramFieldMap.get("RunTest").getText();
		boolean networkAware = false;
		TestTradingDynamicPlacement test = new TestTradingDynamicPlacement();
		consoleArea.append("out put is in C:\\users\\public\\results");
		test.setBeginTime();
		try{
		if (runTest.equalsIgnoreCase("F")){			
			test.doFFDPlacment(problemSize, dynamic, dynamicVmStartingNumber);
		}else if (runTest.equalsIgnoreCase("S")){
			networkAware = paramFieldMap.get("NetworkAwareInMultiple").getText().equalsIgnoreCase("Y");
			test.doTradingPlacment(problemSize, dynamic, networkAware, dynamicVmStartingNumber); 
		}else if (runTest.equalsIgnoreCase("M")){
			networkAware = paramFieldMap.get("NetworkAwareInSingle").getText().equalsIgnoreCase("Y");
			test.doTradingPlacmentAccrossGrps(problemSize, dynamic, networkAware, dynamicVmStartingNumber);
		}
		}catch(Exception e){
			consoleArea.append(" exception happened " + e.getMessage());
		}
		test.showDuration();
		
	}

	private HashMap<String, JTextField> paramFieldMap = new HashMap<String, JTextField>();

	private JPanel createParameterPanel(String[] labels, String[] descs) {
		int numPairs = labels.length;

        //Create and populate the panel.
        JPanel p = new JPanel(new SpringLayout());
        for (int i = 0; i < numPairs; i++) {
            JLabel l = new JLabel(labels[i] + ": ", JLabel.TRAILING);
            p.add(l);
            JTextField textField = new JTextField(10);
            l.setLabelFor(textField);
            p.add(textField);
            JLabel d = new JLabel(descs[i], JLabel.LEFT);
            p.add(d);
            
            paramFieldMap.put(labels[i], textField);
        }

        //Lay out the panel.
        SpringUtilities.makeCompactGrid(p,
                                        numPairs, 3, //rows, cols
                                        6, 6,        //initX, initY
                                        6, 6);       //xPad, yPad
		return p;
	}

	void addCompForTitledBorder(TitledBorder border,
                                String description,
                                int justification,
                                int position,
                                Container container) {
        border.setTitleJustification(justification);
        border.setTitlePosition(position);
        addCompForBorder(border, description,
                         container);
    }

    private JPanel addCompForBorder(Border border,
                          String description,
                          Container container) {
        JPanel comp = new JPanel(false);
        comp.setSize(800, 600);
        comp.setLayout(new BoxLayout(comp,BoxLayout.X_AXIS));
        
        JLabel label = new JLabel(description, JLabel.CENTER);
        comp.add(label);
        comp.setBorder(border);

        container.add(Box.createRigidArea(new Dimension(0, 10)));
        container.add(comp);
        return comp;
    }


    /** Returns an ImageIcon, or null if the path was invalid. */
    protected static ImageIcon createImageIcon(String path,
                                               String description) {
        java.net.URL imgURL = TradingTestUI.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the 
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("Trading VM experiment");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        TradingTestUI newContentPane = new TradingTestUI();
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
