package tools;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class QLearningVisualizer extends ApplicationFrame {

    private XYSeriesCollection stateDataset;
    private XYSeriesCollection actionDataset;
    private XYSeriesCollection rewardDataset;
    private XYSeriesCollection deviceDataset;
    private XYSeries currentStateSeries;
    private XYSeries goalStateSeries;
    private XYSeries actionSeries;
    private XYSeries rewardSeries;
    private XYSeries z1LevelSeries;
    private XYSeries z2LevelSeries;
    private XYSeries z1BlindsSeries;
    private XYSeries z2BlindsSeries;
    private XYSeries z1LightSeries;
    private XYSeries z2LightSeries;
    private XYSeries outdoorLightSeries;

    private JTable qTableDisplay;
    private DefaultTableModel qTableModel;

    private JTextField goalDescriptionField;
    private JTextField episodesField;
    private JTextField alphaField;
    private JTextField gammaField;
    private JTextField epsilonField;
    private JTextField rewardField;
    private JButton startButton;

    private QLearner qLearner;

    public QLearningVisualizer(String title) {
        super(title);
        initComponents();
        setupListeners();
    }

    private void initComponents() {
        stateDataset = new XYSeriesCollection();
        actionDataset = new XYSeriesCollection();
        rewardDataset = new XYSeriesCollection();
        deviceDataset = new XYSeriesCollection();

        currentStateSeries = new XYSeries("Current State");
        goalStateSeries = new XYSeries("Goal State");
        actionSeries = new XYSeries("Action");
        rewardSeries = new XYSeries("Reward");
        z1LevelSeries = new XYSeries("Z1 Level");
        z2LevelSeries = new XYSeries("Z2 Level");
        z1BlindsSeries = new XYSeries("Z1 Blinds");
        z2BlindsSeries = new XYSeries("Z2 Blinds");
        z1LightSeries = new XYSeries("Z1 Light");
        z2LightSeries = new XYSeries("Z2 Light");
        outdoorLightSeries = new XYSeries("Outdoor Light");

        stateDataset.addSeries(currentStateSeries);
        stateDataset.addSeries(goalStateSeries);
        actionDataset.addSeries(actionSeries);
        rewardDataset.addSeries(rewardSeries);
        deviceDataset.addSeries(z1LevelSeries);
        deviceDataset.addSeries(z2LevelSeries);
        deviceDataset.addSeries(z1BlindsSeries);
        deviceDataset.addSeries(z2BlindsSeries);
        deviceDataset.addSeries(z1LightSeries);
        deviceDataset.addSeries(z2LightSeries);
        deviceDataset.addSeries(outdoorLightSeries);

        JFreeChart stateChart = createChart("State Transition", "Step", "State", stateDataset);
        JFreeChart actionChart = createChart("Action Taken", "Step", "Action", actionDataset);
        JFreeChart rewardChart = createChart("Reward Received", "Step", "Reward", rewardDataset);
        JFreeChart deviceChart = createChart("Device States", "Step", "Value", deviceDataset);

        ChartPanel stateChartPanel = new ChartPanel(stateChart);
        stateChartPanel.setPreferredSize(new Dimension(800, 600));
        stateChartPanel.setMouseWheelEnabled(true);

        ChartPanel actionChartPanel = new ChartPanel(actionChart);
        actionChartPanel.setPreferredSize(new Dimension(800, 600));
        actionChartPanel.setMouseWheelEnabled(true);

        ChartPanel rewardChartPanel = new ChartPanel(rewardChart);
        rewardChartPanel.setPreferredSize(new Dimension(800, 600));
        rewardChartPanel.setMouseWheelEnabled(true);

        ChartPanel deviceChartPanel = new ChartPanel(deviceChart);
        deviceChartPanel.setPreferredSize(new Dimension(800, 600));
        deviceChartPanel.setMouseWheelEnabled(true);

        JPanel parameterPanel = new JPanel(new GridLayout(7, 2));
        parameterPanel.setBorder(BorderFactory.createTitledBorder("Q-Learning Parameters"));

        parameterPanel.add(new JLabel("Goal Description:"));
        goalDescriptionField = new JTextField("[2, 3]");
        parameterPanel.add(goalDescriptionField);

        parameterPanel.add(new JLabel("Episodes:"));
        episodesField = new JTextField("1000");
        parameterPanel.add(episodesField);

        parameterPanel.add(new JLabel("Alpha:"));
        alphaField = new JTextField("0.5");
        parameterPanel.add(alphaField);

        parameterPanel.add(new JLabel("Gamma:"));
        gammaField = new JTextField("0.5");
        parameterPanel.add(gammaField);

        parameterPanel.add(new JLabel("Epsilon:"));
        epsilonField = new JTextField("0.1");
        parameterPanel.add(epsilonField);

        parameterPanel.add(new JLabel("Reward:"));
        rewardField = new JTextField("-1"); // Default to -1 for non-goal states
        parameterPanel.add(rewardField);

        startButton = new JButton("Start Learning");
        parameterPanel.add(startButton);

        JPanel chartPanel = new JPanel(new GridLayout(2, 2));
        chartPanel.add(stateChartPanel);
        chartPanel.add(actionChartPanel);
        chartPanel.add(rewardChartPanel);
        chartPanel.add(deviceChartPanel);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(chartPanel, BorderLayout.CENTER);
        mainPanel.add(parameterPanel, BorderLayout.EAST);

        JPanel qTablePanel = createQTablePanel();
        qTablePanel.setPreferredSize(new Dimension(800, 200)); // Set preferred height to be 1/3 of the chart panels' height
        mainPanel.add(qTablePanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);

        this.pack();
        RefineryUtilities.centerFrameOnScreen(this);
        this.setVisible(true);

        // Initialize the QLearner instance
        qLearner = new QLearner();
    }

    private JFreeChart createChart(String title, String xAxisLabel, String yAxisLabel, XYSeriesCollection dataset) {
        JFreeChart chart = ChartFactory.createXYLineChart(
                title,
                xAxisLabel,
                yAxisLabel,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        plot.setRenderer(renderer);
        return chart;
    }

    private JPanel createQTablePanel() {
        qTableModel = new DefaultTableModel();
        qTableDisplay = new JTable(qTableModel);
        JScrollPane scrollPane = new JScrollPane(qTableDisplay);
        qTableDisplay.setFillsViewportHeight(true);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createTitledBorder("Q-Table"));
        return panel;
    }

    private void setupListeners() {
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetAndStartLearning();
            }
        });
    }

    private void resetAndStartLearning() {
        // Clear existing data series
        currentStateSeries.clear();
        goalStateSeries.clear();
        actionSeries.clear();
        rewardSeries.clear();
        z1LevelSeries.clear();
        z2LevelSeries.clear();
        z1BlindsSeries.clear();
        z2BlindsSeries.clear();
        z1LightSeries.clear();
        z2LightSeries.clear();
        outdoorLightSeries.clear();
        
        // Clear Q-Table
        qTableModel.setRowCount(0);
        
        // Extract parameters and start learning
        startLearning();
    }

    private void startLearning() {
        try {
            String goalDescriptionStr = goalDescriptionField.getText();
            String[] goalDescriptionArr = goalDescriptionStr.replace("[", "").replace("]", "").split(",");
            Integer[] goalDescription = new Integer[goalDescriptionArr.length];
            for (int i = 0; i < goalDescriptionArr.length; i++) {
                goalDescription[i] = Integer.parseInt(goalDescriptionArr[i].trim());
            }

            int episodes = Integer.parseInt(episodesField.getText());
            double alpha = Double.parseDouble(alphaField.getText());
            double gamma = Double.parseDouble(gammaField.getText());
            double epsilon = Double.parseDouble(epsilonField.getText());
            int reward = Integer.parseInt(rewardField.getText());

            // Call calculateQ method with parameters
            qLearner.calculateQ(goalDescription, episodes, alpha, gamma, epsilon, reward);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid input: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void update(int step, List<Integer> currentState, List<Integer> goalState, int action, double reward) {
        currentStateSeries.add(step, currentState.get(0));
        goalStateSeries.add(step, goalState.get(0));
        actionSeries.add(step, action);
        rewardSeries.add(step, reward);

        z1LevelSeries.add(step, currentState.get(0));
        z2LevelSeries.add(step, currentState.get(1));
        z1BlindsSeries.add(step, currentState.get(4));
        z2BlindsSeries.add(step, currentState.get(5));
        z1LightSeries.add(step, currentState.get(2));
        z2LightSeries.add(step, currentState.get(3));
        outdoorLightSeries.add(step, currentState.get(6));
    }

    public void updateQTable(double[][] qTable) {
        String[] columnNames = new String[qTable[0].length];
        for (int i = 0; i < qTable[0].length; i++) {
            columnNames[i] = "Action " + i;
        }

        String[][] data = new String[qTable.length][qTable[0].length];
        for (int i = 0; i < qTable.length; i++) {
            for (int j = 0; j < qTable[i].length; j++) {
                data[i][j] = String.format("%.2f", qTable[i][j]);
            }
        }

        qTableModel.setDataVector(data, columnNames);

        // Set custom renderer to display heatmap in the Q-table
        qTableDisplay.setDefaultRenderer(Object.class, new HeatMapCellRenderer());
    }

    private static class HeatMapCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof Number) {
                double val = ((Number) value).doubleValue();
                c.setBackground(getHeatMapColor(val));
                setText(String.format("%.2f", val));  // Display the number
            } else {
                c.setBackground(Color.WHITE);
            }
            return c;
        }

        private Color getHeatMapColor(double value) {
            int green = (int) ((1 - value) * 255);
            int red = (int) (value * 255);
            return new Color(red, green, 0);
        }
    }
}
