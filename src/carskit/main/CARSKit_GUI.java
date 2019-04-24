package carskit.main;
import carskit.main.CARSKit;
import javax.swing.*;

public class CARSKit_GUI {
	static CARSKit kit;
	
	static void createFrame() {
		JFrame f=new JFrame();//creating instance of JFrame  
        
		JButton b=new JButton("click");//creating instance of JButton  
		b.setBounds(130,100,100, 40);//x axis, y axis, width, height  
		          
		f.add(b);//adding button in JFrame  
		          
		f.setSize(400,500);//400 width and 500 height  
		f.setLayout(null);//using no layout managers  
		f.setVisible(true);//making the frame visible 
	}

	public static void main(String[] args) {
		kit = new CARSKit();
		createFrame();
		

		// TODO Auto-generated method stub
	}

}
