import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class listener implements Runnable {

	Socket sock=null;
	BufferedReader recieve=null;
	public listener(Socket socket){
		this.sock=socket;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			recieve = new BufferedReader(new InputStreamReader(this.sock.getInputStream()));
			String incoming = "";
			while((incoming = recieve.readLine()) != null) { 
				System.out.println(incoming + "\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}