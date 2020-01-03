package simulationForRestaurant;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimulateRestaurantQueue extends Application {
	// private static long startTime = System.currentTimeMillis();
	private static final int NUMBER_OF_INTERVAL = 48;// the number of interval that customer will come 
	//and each interval represents 5 minutes
														
	private static final int INTERVAL_SIZE = 1000; // interval size
	private static LinkedList<Customer> queue = new LinkedList<>(); // use LinkList to simulate queue
	private static LinkedList<Customer> allCustomer = new LinkedList<>(); // use LinkList to store all customer

	private static Thread simulateArriving = new Thread(new ArrivingThread()); // a thread simulating arriving process
	private static Thread simulateLeaving = new Thread(new LeavingThread()); // a thread simulating leaving process
	private static int customerCount = 1; //customer counter

	private static double arrivingLambda  = 4.6; // arriving time obey to poisson distribution
	//, and arrivingLamuda means the number of customer comes in each interval  
	private static final double INCREMENT = 0.25; // lambda increment

	private static double serivicingLambda  = 50; // serivicing time obey to poisson distribution
	//, and serivicingLambda means the time cost for seriving each customer  
	private static final double DECREMENT = 2; // lambda decrement

	private static Circle[] graphDot = new Circle[NUMBER_OF_INTERVAL * 2]; // the dot in the queueLength-time graph
	private static Line[] graphLine = new Line[graphDot.length - 1]; // the line connects each dots
	private static Text[] queueSize = new Text[graphDot.length - 1]; // the text tells queueSize of each dots
	private static int[] queueLength = new int[graphDot.length - 1]; // store queueLength of every interval
	private static int[] numberOfWindows = new int[NUMBER_OF_INTERVAL / 2]; // store the windows open for customers

	private static Pane pane = new Pane(); // display queueLength-time graph
	private static Scene scene;

	private static Lock lock = new ReentrantLock();

	public static void main(String[] args) {
		// set thread name
		simulateArriving.setName("SimulateArrivingThread");
		simulateLeaving.setName("SimulateLeavingThread");
		launch(args);// launch javafx
	}

	//randomly get a number following poisson distribution
	private static int getPoissonVariable(double lambda) {
		int x = 0;
		double y = Math.random(), cdf = getPoissonProbability(x, lambda);
		while (cdf < y) {
			x++;
			cdf += getPoissonProbability(x, lambda);
		}
		return x;
	}

	//formulate of poisson distribution
	static double getPoissonProbability(int n, double lambda) {
		double p;
		p = Math.exp(-lambda) * Math.pow(lambda, n) / factorial(n);
		return p;
	}

	// calculate factorial
	static long factorial(int k) {
		long result = 1;
		for (int i = 2; i <= k; i++) {
			result *= i;
		}
		return result;
	}

	// display interface of simulating with 4 functions
	@Override
	public void start(Stage stage) throws Exception {
		pane.setPrefSize(NUMBER_OF_INTERVAL * 25, 870);
		BorderPane borderPane = new BorderPane();

		// menuBar
		MenuBar menuBar = new MenuBar();
		menuBar.prefWidthProperty().bind(stage.widthProperty());
		borderPane.setTop(menuBar);
		Menu graph1 = new Menu("The length of queue in every 5 minutes");
		MenuItem graphPart = new MenuItem("open");
		graph1.getItems().add(graphPart);

		Menu graph2 = new Menu("The waiting time of each customer ");
		MenuItem graph2Part = new MenuItem("open");
		graph2.getItems().add(graph2Part);

		Menu table1 = new Menu("Information of each customer");
		MenuItem tablePart = new MenuItem("open");
		table1.getItems().add(tablePart);

		Menu table2 = new Menu("Optimum proposal for restaurant manager");
		MenuItem table2Part = new MenuItem("open");
		table2.getItems().add(table2Part);

		menuBar.getMenus().addAll(graph1, graph2, table1, table2);

		// element of graph
		HBox pane4Bt = new HBox();
		Line xAxis = new Line(15, pane.getPrefHeight() - 5, pane.getPrefWidth() - 80, pane.getPrefHeight() - 5);
		Line yAxis = new Line(15, pane.getPrefHeight() - 5, 15, 80);
		Text xLabel = new Text("Time/ 5 mins");
		Text yLabel = new Text("The length of queue from 10:00 to 14:00/ people");

		// set element attribution and add to pane
		xAxis.setStrokeWidth(2);
		yAxis.setStrokeWidth(2);
		xLabel.setStyle("-fx-font: 24 arial");
		xLabel.setX(xAxis.getEndX() - 50);
		xLabel.setY(xAxis.getEndY() - 20);
		yLabel.setStyle("-fx-font: 24 arial");
		yLabel.setX(yAxis.getEndX() + 10);
		yLabel.setY(yAxis.getEndY() + 20);
		pane.getChildren().add(xAxis);
		pane.getChildren().add(yAxis);
		pane.getChildren().add(xLabel);
		pane.getChildren().add(yLabel);

		// initial graph dots
		for (int i = 0; i < graphDot.length; i++) {
			graphDot[i] = new Circle();
			pane.getChildren().add(graphDot[i]);
		}

		// initial graph texts
		for (int i = 0; i < queueSize.length; i++) {
			queueSize[i] = new Text();
			pane.getChildren().add(queueSize[i]);
		}

		// initial graph lines
		for (int i = 0; i < graphLine.length; i++) {
			graphLine[i] = new Line();
			pane.getChildren().add(graphLine[i]);
		}

		Button btStart = new Button("START");
		btStart.setTextFill(Color.RED);
		btStart.setWrapText(true);
		btStart.setStyle("-fx-font: 32 arial");
		btStart.setAlignment(Pos.CENTER);
		btStart.setMaxSize(160, 30);

		// start button action, and simulate the length of queue in every 5 minutes
		btStart.setOnAction(event -> {

			System.out.println("The size of queue");
			simulateArriving.start();
			simulateLeaving.start();
			Thread drawGraph = new Thread(new drawGraphThread());
			drawGraph.start();
			btStart.setDisable(true);
			for (int i = 0; i < allCustomer.size(); i++) {
				System.out.println(i + " customer arrived at" + allCustomer.get(i).getArrivingTime());
				System.out.println("and " + i + "customer left at" + allCustomer.get(i).getLeavingTime());
			}
		});

		//the 1st function
		graphPart.setOnAction(e -> {
			borderPane.getChildren().clear();
			// allCustomer.clear();
			borderPane.setTop(menuBar);
			borderPane.setCenter(pane);
			borderPane.setBottom(pane4Bt);
		});

		// the 2nd function: create a new window to show waiting time graph for each customer
		graph2Part.setOnAction(e -> {
			showWaitingTime();
		});

		table2Part.setOnAction(e -> {
			borderPane.getChildren().clear();
			borderPane.setTop(menuBar);
			
			
			TableView<Window> time2Table = new TableView<Window>();
			ObservableList<Window> data2 = FXCollections.observableArrayList();

			time2Table.setMaxWidth(500);
			time2Table.setPrefHeight(500);

			TableColumn<Window, String> timeQuantumColumn = new TableColumn<Window, String>("Time quantum");
			timeQuantumColumn.setPrefWidth(250);
			timeQuantumColumn.setCellValueFactory(new PropertyValueFactory<>("timeQuantum"));

			TableColumn<Window, String> numberOfWindowsColumn = new TableColumn<Window, String>("Number of windows");
			numberOfWindowsColumn.setPrefWidth(250);
			numberOfWindowsColumn.setCellValueFactory(new PropertyValueFactory<>("numberOfWindows"));
			
			String[] timeQuantum={"10:00-10:10","10:10-10:20","10:20-10:30","10:30-10:40","10:40-10:50","10:50-11:00",
					"11:00-11:10","11:10-11:20","11:20-11:30","11:30-11:40","11:40-11:50","11:50-12:00",
					"12:00-12:10","12:10-12:20","12:20-12:30","12:30-12:40","12:40-12:50","12:50-13:00",
					"13:00-13:10","13:10-13:20","13:20-13:30","13:30-13:40","13:40-13:50","13:50-14:00",
			}; // store time quantum of each 5 10 minutes
			
			//initiate numberOfWindows
			for (int i = 0; i < NUMBER_OF_INTERVAL/2; i++) {
				numberOfWindows[i]=1;
			}
			
			//get the number of windows to open in every 10 minutes
			int countOfMaxLength=0;
			for (int i = 0; i < NUMBER_OF_INTERVAL; i=i+2) {
				//find the max length of queue in every 10 minutes
				if(queueLength[i]>=queueLength[i+1]){
					if(queueLength[i]>10)//check whether the max length is bigger than 10
						numberOfWindows[countOfMaxLength]=queueLength[i]/10; //divide it by 10 and get the number of windows to open
				}
				else{
					if(queueLength[i]>10)
						numberOfWindows[countOfMaxLength]=queueLength[i+1]/10;
				}
				countOfMaxLength++;
			}
			
			for (int i = 0; i < NUMBER_OF_INTERVAL/2; i++) {
				data2.add(new Window(timeQuantum[i],numberOfWindows[i]+""));
				time2Table.refresh();
			}
			
			time2Table.setItems(data2);
			time2Table.getColumns().addAll(timeQuantumColumn, numberOfWindowsColumn);

			final VBox vbox = new VBox();
			vbox.setAlignment(Pos.CENTER);
			vbox.setSpacing(5);
			vbox.setPadding(new Insets(10, 0, 0, 10));
			vbox.getChildren().addAll(time2Table);

			borderPane.setCenter(vbox);
		});
		
		// the 3rd function: show a table which has each customer's information
		tablePart.setOnAction(e -> {
			borderPane.getChildren().clear();
			borderPane.setTop(menuBar);

			TableView<Customer> timeTable = new TableView<Customer>();
			ObservableList<Customer> data = FXCollections.observableArrayList();

			timeTable.setMaxWidth(1150);
			timeTable.setPrefHeight(800);

			TableColumn<Customer, String> customerNumber = new TableColumn<Customer, String>("Customer id");
			customerNumber.setPrefWidth(150);
			customerNumber.setCellValueFactory(new PropertyValueFactory<>("id"));

			TableColumn<Customer, String> arrivalTime = new TableColumn<Customer, String>("Arriving time");
			arrivalTime.setPrefWidth(250);
			arrivalTime.setCellValueFactory(new PropertyValueFactory<>("arrivingTime"));

			TableColumn<Customer, String> waitingTime = new TableColumn<Customer, String>("Waiting time");
			waitingTime.setPrefWidth(250);
			waitingTime.setCellValueFactory(new PropertyValueFactory<>("waitingTime"));

			TableColumn<Customer, String> servicingTime = new TableColumn<Customer, String>("Servicing time");
			servicingTime.setPrefWidth(250);
			servicingTime.setCellValueFactory(new PropertyValueFactory<>("servicingTime"));

			TableColumn<Customer, String> leavingTime = new TableColumn<Customer, String>("Leaving time");
			leavingTime.setPrefWidth(250);
			leavingTime.setCellValueFactory(new PropertyValueFactory<>("leavingTime"));

			for (int i = 0; i < allCustomer.size(); i++) {
				data.add(new Customer(allCustomer.get(i).getId(), allCustomer.get(i).getArrivingTime(),
						allCustomer.get(i).getWaitingTime(), allCustomer.get(i).getServicingTime(),
						allCustomer.get(i).getLeavingTime()));

				timeTable.refresh();
			}
			timeTable.setItems(data);
			timeTable.getColumns().addAll(customerNumber, arrivalTime, waitingTime, servicingTime, leavingTime);

			final VBox vbox = new VBox();
			vbox.setAlignment(Pos.CENTER);
			vbox.setSpacing(5);
			vbox.setPadding(new Insets(10, 0, 0, 10));
			vbox.getChildren().addAll(timeTable);

			borderPane.setCenter(vbox);
		});

		pane4Bt.getChildren().add(btStart);
		pane4Bt.setAlignment(Pos.CENTER);
		pane4Bt.setPadding(new Insets(10));
		borderPane.setCenter(pane);
		borderPane.setBottom(pane4Bt);

		scene = new Scene(borderPane);
		stage.setScene(scene);
		stage.setResizable(false);
		stage.setTitle("Simulate Restaurant Queue");
		stage.show();
	}

	// simulating arriving process thread
	public static class ArrivingThread implements Runnable {
		int timeIntervalCount = 0;
		int id = 0;
		Double moment = 0.0;
		int currentHour = 10;
		int currentMintue = 0;

		@Override
		public void run() {
			long startTime = System.currentTimeMillis(); //record start time of simulate
			try {
				while (timeIntervalCount < NUMBER_OF_INTERVAL) {
					int numOfCustomer = getPoissonVariable(arrivingLambda ); // get number of customer that will come in a interval
					if (numOfCustomer == 0) {
						numOfCustomer = 1; // at least one customer will come
					}
					// add a customer into queue in each loop
					for (int i = 1; i <= numOfCustomer; i++) {
						Customer newCustomer = new Customer(String.valueOf(customerCount)); // set id
						moment = (double) Math.round((System.currentTimeMillis() - startTime) / ((double) 1000) * 100)/ 100; // get arrival interval positon
						currentHour = (int) ((moment) * 5 / 60); //get current Hour
						currentMintue = (int) (moment * 5 - 60 * currentHour); //get current Mintues

						newCustomer.setArrivingTime(10 + currentHour + ":" + currentMintue); // set arrival time
						customerCount += 1;
						
						lock.lock();
						queue.add(newCustomer);
						lock.unlock();

						Thread.sleep(INTERVAL_SIZE / numOfCustomer); // the period between each customer's arrival
					}
					System.out.println("The " + timeIntervalCount + "th period.");
					
                    // plus or minus increment
					if (timeIntervalCount < NUMBER_OF_INTERVAL / 2) {
						arrivingLambda  += INCREMENT;
					} else {
						arrivingLambda  -= INCREMENT;
					}
					timeIntervalCount++;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
    // simulating leaving process thread
	public static class LeavingThread implements Runnable {
		int timeIntervalCount = 0;
		int count = 1;
		Double moment = 0.0;
		int currentHour = 10;
		int currentMintues = 0;
		int waitingTime = 0;
		int servicingTime = 0;
		@Override
		public void run() {
			long startTime = System.currentTimeMillis();
			try {
				while (true) {
					if (!simulateArriving.isAlive() && queue.size() == 0) {
						return;
					}
					if (queue.isEmpty()) {
						Thread.sleep(5);
					} else {
						moment = (double) Math.round((System.currentTimeMillis() - startTime) / ((double) INTERVAL_SIZE) * 100) / 100;
						currentHour = 10 + (int) ((moment) * 5 / 60);
						currentMintues = (int) (moment * 5 - 60 * (currentHour - 10));
						
						//get waitingTime and waitingTime of each customer =the time cost for walking the whole queue
						waitingTime = (currentHour - Integer.parseInt(queue.get(0).getArrivingTime().substring(0, 2)))* 60 + currentMintues - Integer.parseInt(queue.get(0).getArrivingTime().substring(3));
						queue.get(0).setWaitingTime(waitingTime + " minutes"); //set waitingTime
						
						// minus or plus decrement for serivicingLambda
						timeIntervalCount = (int) ((System.currentTimeMillis() - startTime) / INTERVAL_SIZE);
						if (timeIntervalCount == count) {
							count++;
							if (timeIntervalCount <= NUMBER_OF_INTERVAL / 2) {
								serivicingLambda  = serivicingLambda  - DECREMENT;
							} else {
								serivicingLambda  = 2 + (timeIntervalCount - 24) * DECREMENT;
							}
						}
						
						servicingTime = (120 + getPoissonVariable(serivicingLambda )); // service time obey to poisson
						System.out.println("La" + serivicingLambda );
						
						Thread.sleep(servicingTime); // after service time, we can get leaving time
						
						lock.lock();
						queue.get(0).setServicingTime(servicingTime * 5 / ((double) INTERVAL_SIZE) + " minutes");
						lock.unlock();//set service time
						
						moment = (double) Math
								.round((System.currentTimeMillis() - startTime) / ((double) INTERVAL_SIZE) * 100) / 100;
						currentHour = 10 + (int) ((moment) * 5 / 60);
						currentMintues = (int) Math.round(moment * 5 - 60 * (currentHour - 10));

						queue.get(0).setLeavingTime(currentHour + ":" + currentMintues);//set LeavingTime
						allCustomer.add(queue.removeFirst()); //first customer leave the queue and allCustomer+1
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	// a thread used to draw graph
	public static class drawGraphThread implements Runnable {

		@Override
		public void run() {
			try {
				int count = 0;
                // set dots position for each interval
				while (true) {
					if (!simulateLeaving.isAlive() && queue.size() == 0) {
						graphDot[count].setCenterX(count * 15 + 15);
						graphDot[count].setCenterY(pane.getPrefHeight() - 5 - queue.size() * 10);
						graphDot[count].setRadius(3);
						queueLength[count]=queue.size();
						queueSize[count].setText(queue.size() + "");
						queueSize[count].setX(count * 15 + 10);
						queueSize[count].setY(pane.getPrefHeight() - 5 - queue.size() * 10 - 10);
						break;
					}
					graphDot[count].setCenterX(count * 15 + 15);
					graphDot[count].setCenterY(pane.getPrefHeight() - 5 - queue.size() * 10);
					graphDot[count].setRadius(3);
					queueLength[count]=queue.size();
					queueSize[count].setText(queue.size() + "");
					queueSize[count].setX(count * 15 + 10);
					queueSize[count].setY(pane.getPrefHeight() - 5 - queue.size() * 10 - 10);
					System.out.println("size" + queue.size());
					count++;
					Thread.sleep(1000);
				}
                // link each dots
				for (int i = 0; i < graphLine.length; i++) {
					if (graphDot[i + 1].getCenterX() == 0) {
						break;
					}
					graphLine[i].setStartX(graphDot[i].getCenterX());
					graphLine[i].setStartY(graphDot[i].getCenterY());
					graphLine[i].setEndX(graphDot[i + 1].getCenterX());
					graphLine[i].setEndY(graphDot[i + 1].getCenterY());
					Thread.sleep(50);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private static void showWaitingTime() {
		Stage showWaitingTimeGraph = new Stage();
		Pane waitingTimeGraph = new Pane();
		HBox timeColumnContent = new HBox();

		waitingTimeGraph.setPrefSize(1800, pane.getPrefHeight());
		timeColumnContent.setPrefSize(1800, waitingTimeGraph.getPrefHeight());
		timeColumnContent.setPadding(new Insets(0, 15, 15, 15));
		timeColumnContent.setAlignment(Pos.BOTTOM_LEFT);
		waitingTimeGraph.getChildren().add(timeColumnContent);

		// add time column into graph
		double culWidth = ((timeColumnContent.getPrefWidth() - 80) / allCustomer.size() - 1);
		for (int i = 0; i < allCustomer.size(); i++) {
			Customer tempCus = allCustomer.get(i);
			Rectangle timeColumn = new Rectangle(culWidth,
					(Double.parseDouble(tempCus.getWaitingTime().substring(0, tempCus.getWaitingTime().length() - 7)))
							* 15);
			timeColumn.setFill(Color.LIGHTBLUE);
			timeColumn.setStrokeWidth(0.4);
			timeColumn.setStroke(Color.WHITE);
			timeColumnContent.getChildren().add(timeColumn);
		}

		// element of graph
		Line xAxis = new Line(15, waitingTimeGraph.getPrefHeight() - 15, waitingTimeGraph.getPrefWidth() - 15,
				waitingTimeGraph.getPrefHeight() - 15);
		Line yAxis = new Line(xAxis.getStartX(), xAxis.getStartY(), 15, 15);
		Line yArrowLeft = new Line(yAxis.getEndX(), yAxis.getEndY(), yAxis.getEndX() - 10, yAxis.getEndY() + 10);
		Line yArrowRight = new Line(yAxis.getEndX(), yAxis.getEndY(), yAxis.getEndX() + 10, yAxis.getEndY() + 10);
		Line xArrowTop = new Line(xAxis.getEndX(), xAxis.getEndY(), xAxis.getEndX() - 10, xAxis.getEndY() + 10);
		Line xArrowBottom = new Line(xAxis.getEndX(), xAxis.getEndY(), xAxis.getEndX() - 10, xAxis.getEndY() - 10);
		Text xLabel = new Text("customer counter");
		Text yLabel = new Text("The waiting time of customer");

		// attribution
		xLabel.setStyle("-fx-font: 24 arial");
		xLabel.setX(xAxis.getEndX() - 180);
		xLabel.setY(xAxis.getEndY() - 20);
		yLabel.setStyle(xLabel.getStyle());
		yLabel.setX(yAxis.getEndX() + 10);
		yLabel.setY(yAxis.getEndY() + 20);
		waitingTimeGraph.getChildren().addAll(xAxis, yAxis, xArrowTop, xArrowBottom, yArrowLeft, yArrowRight, xLabel,
				yLabel);

		Scene scene = new Scene(waitingTimeGraph);
		showWaitingTimeGraph.setScene(scene);
		showWaitingTimeGraph.setTitle("Simulate Restaurant Queue");
		showWaitingTimeGraph.show();
	}
}
