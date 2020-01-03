package simulationForRestaurant;

public class Customer {
    private String id;
    private String arrivingTime;
    private String waitingTime;
    private String servicingTime;
    private String leavingTime;


    Customer(String id){
        this.id = id;
    }
    
    Customer(String id, String arrivingTime, String waitingTime , String servicingTime, String leavingTime) {
		this.id = new String(id);
		this.arrivingTime = new String(arrivingTime);
		this.waitingTime = new String(waitingTime);
		this.servicingTime = new String(servicingTime);
		this.leavingTime = new String(leavingTime);
	}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getArrivingTime() {
        return arrivingTime;
    }

    public void setArrivingTime(String arrivingTime) {
        this.arrivingTime = arrivingTime;
    }
    
    public String getWaitingTime() {
        return waitingTime;
    }

    public void setWaitingTime(String waitingTime) {
        this.waitingTime = waitingTime;
    }

    public String getServicingTime() {
        return servicingTime;
    }

    public void setServicingTime(String servicingTime) {
        this.servicingTime = servicingTime;
    }
    
    public String getLeavingTime() {
        return leavingTime;
    }

    public void setLeavingTime(String leavingTime) {
        this.leavingTime = leavingTime;
    }
}
