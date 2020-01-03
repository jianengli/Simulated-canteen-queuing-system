package simulationForRestaurant;

public class Window {

    private String timeQuantum;
    private String queueLength;
    private String numberOfWindows;


    Window(String timeQuantum, String numberOfWindows) {
		this.timeQuantum = new String(timeQuantum);
		this.numberOfWindows = new String(numberOfWindows);
	}

    public String getTimeQuantum() {
        return timeQuantum;
    }

    public void setTimeQuantum(String id) {
        this.timeQuantum = timeQuantum;
    }
    
    public String getNumberOfWindows() {
        return numberOfWindows;
    }

    public void setNumberOfWindows(String id) {
        this.numberOfWindows = numberOfWindows;
    }
}
