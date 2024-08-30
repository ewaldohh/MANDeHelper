import java.awt.EventQueue;

import javax.swing.*;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.Font;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import oracle.stellent.ridc.IdcClient;
import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.IdcClientManager;
import oracle.stellent.ridc.IdcContext;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.TransferFile;
import oracle.stellent.ridc.model.serialize.HdaBinderSerializer;
import oracle.stellent.ridc.protocol.ServiceResponse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MainWindow {

	private JFrame frmMandeHelper;
	private JTextField textField_1;
	private JTextField textField_2;
	
	private static File myObj;
	private static File downloadPath;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
					window.frmMandeHelper.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public static void CombineDoc() {
		try {
//          File myObj = new File("C:\\Users\\AL0512633\\Export\\document_number.txt");
          Scanner myReader = new Scanner(myObj);
          while (myReader.hasNextLine()) {
              String data = myReader.nextLine();
              System.out.println(data);
              try {
                  List<String> docList = new ArrayList<String>();
                  List<String> ddocnameList = new ArrayList<String>();
                  List<String> permissionList = new ArrayList<String>();
                  List<String> typeList = new ArrayList<String>();
                  IdcClientManager manager = new IdcClientManager();
//                  IdcClient client = manager.createClient("idc://idphmbpn-apet02");
                  IdcClient client = manager.createClient("idc://idphmbpn-adet01");
//                  IdcContext connectionContext = new IdcContext("mande_admins");
                  IdcContext connectionContext = new IdcContext("weblogic");
                  String getDocQuery =
                      "select document_id,ddocname,did,ddocnumber,file_type,dsecuritygroup from etdms_document where ddocnumber = ? and dstatus = 'ISSUED'\n" +
                      "ORDER BY DREVISIONDATE,DUPLOADDATE";

                  String ddocnumber = data;
//                  String downloadPath = "C:\\Users\\AL0512633\\Export\\";
                  String FileName = "";
                  String dDocName = "";
                  String dID = "";
                  Connection con = getConnection();
                  PreparedStatement pst = con.prepareStatement(getDocQuery);
                  pst.setString(1, ddocnumber);
                  ResultSet rs = null;
                  rs = pst.executeQuery();
                  while (rs.next()) {
                      System.out.println(rs.getString(1) + " " + rs.getString(2) + " " + rs.getString(3));
                      FileName = rs.getString(1) + "." + rs.getString(5);
                      dDocName = rs.getString(2);
                      dID = rs.getString(3);
                      docList.add(rs.getString(1));
                      ddocnameList.add(rs.getString(2));
                      typeList.add(rs.getString(5));
                      permissionList.add(rs.getString(6));
                      System.out.println("download start");

                      HdaBinderSerializer serializer = new HdaBinderSerializer("UTF-8", client.getDataFactory());

                      // Databinder for checkin request
                      DataBinder dataBinder = client.createBinder();
                      dataBinder.putLocal("IdcService", "GET_FILE");
                      dataBinder.putLocal("dID", dID);
                      dataBinder.putLocal("dDocName", dDocName);
                      dataBinder.putLocal("allowInterrupt", "1");

                      //     serializer.serializeBinder(System.out, dataBinder);

                      // Create an output stream to output the file received
                      String path = downloadPath + FileName;
                      FileOutputStream fos = new FileOutputStream(path);
                      // Send the request to Content Server
                      ServiceResponse response = client.sendRequest(connectionContext, dataBinder);

                      // Create an input stream from the response
                      InputStream fis = response.getResponseStream();
                      // Read the data in 1KB chunks and write it to the file
                      byte[] readData = new byte[1024];
                      int i = fis.read(readData);
                      while (i != -1) {
                          fos.write(readData, 0, i);
                          i = fis.read(readData);
                      }

                      // Close the socket connection
                      response.close();
                      // Don't leave the streams open
                      fos.close();
                      fis.close();
                      System.out.println("download finish");
                  }
                  rs.close();
                  pst.close();
                  String ddocnameNew = "";
                  for (int i = 0; i < docList.size(); i++) {
                      DataBinder binder = client.createBinder();
                      binder.putLocal("IdcService", "CHECKIN_UNIVERSAL");
                      if (i > 0) {
                          System.out.println("check out " + ddocnameNew);
                          DataBinder binder2 = client.createBinder();
                          binder2.putLocal("IdcService", "CHECKOUT_BY_NAME");

                          binder2.putLocal("dDocName", ddocnameNew);

                          // Check Out
                          ServiceResponse response2 = client.sendRequest(connectionContext, binder2);
                          DataBinder responseBinder2 = response2.getResponseAsBinder();
                          binder.putLocal("dDocName", ddocnameNew);
                      }

                      binder.putLocal("dSecurityGroup", permissionList.get(i));
                      binder.putLocal("dDocAuthor", "weblogic");
                      binder.putLocal("dDocTitle", docList.get(i));
                      binder.putLocal("dDocType", "pf_tech_doc");
                      Integer rev = i + 1;
                      binder.putLocal("dRevLabel", rev.toString());
                      //                populateDataBinder(binder, documentRow);

                      // File
                      File newFile = new File(downloadPath + docList.get(i) + "." + typeList.get(i));
                      TransferFile transferFile = new TransferFile(newFile);
                      transferFile.setFileName(docList.get(i) + "." + typeList.get(i));

                      binder.addFile("primaryFile", transferFile);
                      System.out.println("check in");
                      System.out.println(binder);

                      // Checkin
                      ServiceResponse response = client.sendRequest(connectionContext, binder);

                      // Get response
                      DataBinder responseBinder = response.getResponseAsBinder();
                      System.out.println(responseBinder);
                      ddocnameNew = responseBinder.getLocal("dDocName");
                      System.out.println("update metadata " + docList.get(i));
                      if (i < docList.size() - 1)
                          pst =
                              con.prepareStatement("update etdms_document set ddocname = ?,did=?,drevisionnumber =?,is_primary ='N' where document_id = ?");
                      else
                          pst =
                              con.prepareStatement("update etdms_document set ddocname = ?,did=?,drevisionnumber =?, is_primary ='Y' where document_id = ?");
                      pst.setString(1, ddocnameNew);
                      pst.setString(2, responseBinder.getLocal("dID"));
                      pst.setString(3, responseBinder.getLocal("dRevLabel"));
                      pst.setString(4, docList.get(i));
                      pst.executeUpdate();
                      pst.close();
                      if (i < docList.size() - 1) {
                          System.out.println("Delete from folder files " + docList.get(i));
                          pst = con.prepareStatement("delete from etdms_folder_files where document_id = ?");
                          pst.setString(1, docList.get(i));
                          pst.executeUpdate();
                          pst.close();
                      }
                  }
                  con.close();

                  System.out.println("File combine successfully");
                  //delete doc
                  for (int i = 0; i < ddocnameList.size(); i++) {
                      try {
                          System.out.println("start delete : " + ddocnameList.get(i));
                          // Databinder for checkin request
                          DataBinder dataBinder = client.createBinder();
                          dataBinder.putLocal("IdcService", "DELETE_DOC");
                          dataBinder.putLocal("dDocName", ddocnameList.get(i));

                          client.sendRequest(connectionContext, dataBinder);
                          System.out.println("File deleted successfully");
                      } catch (Exception e) {
                          e.printStackTrace();
                      }
                  }
              } catch (Exception e) {
                  e.printStackTrace();
              }

          }
          myReader.close();
      } catch (Exception e) {
          e.printStackTrace();
      }
	}
	
    private static Connection getConnection() {
        Connection connection = null;
        
        try {
//            String driver = "oracle.jdbc.driver.OracleDriver";
//            String url = "jdbc:oracle:thin:@srv-oradb-un11:1571:ETDMSPRD";
//            String username = "ETDMS_PROD";
//            String password = "ETDMS_PRODPWD";
            
            String driver = "oracle.jdbc.driver.OracleDriver";
            String url = "jdbc:oracle:thin:@PHMORADEV01:1571:ETDMSDB";
            String username = "ETDMS_DEV";
            String password = "welcome1";
            Class.forName(driver);
            connection = DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connection;
    }

	/**
	 * Create the application.
	 */
	public MainWindow() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmMandeHelper = new JFrame();
		frmMandeHelper.setTitle("MANDe Helper");
		frmMandeHelper.setBounds(100, 100, 450, 300);
		frmMandeHelper.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmMandeHelper.getContentPane().setLayout(null);
		
		JPanel panel = new JPanel();
		panel.setBackground(new Color(255, 255, 255));
		panel.setBounds(0, 0, 436, 263);
		frmMandeHelper.getContentPane().add(panel);
		panel.setLayout(null);
		
		JLabel lblNewLabel = new JLabel("Combine Document");
		lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 16));
		lblNewLabel.setBounds(142, 28, 158, 14);
		panel.add(lblNewLabel);
		
		JLabel lblNewLabel_2 = new JLabel("List Document Number");
		lblNewLabel_2.setFont(new Font("Tahoma", Font.PLAIN, 11));
		lblNewLabel_2.setBounds(28, 110, 113, 14);
		panel.add(lblNewLabel_2);
		
		JLabel lblNewLabel_3 = new JLabel("Export Document");
		lblNewLabel_3.setFont(new Font("Tahoma", Font.PLAIN, 11));
		lblNewLabel_3.setBounds(28, 149, 86, 14);
		panel.add(lblNewLabel_3);
		
		textField_1 = new JTextField();
		textField_1.setBounds(144, 107, 158, 20);
		panel.add(textField_1);
		textField_1.setColumns(10);
		
		textField_2 = new JTextField();
		textField_2.setBounds(144, 146, 158, 20);
		panel.add(textField_2);
		textField_2.setColumns(10);
		
		JButton btnNewButton = new JButton("Confirm");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (textField_1.getText().trim().isEmpty() || textField_2.getText().trim().isEmpty()) {
                    // Show error message if either text field is empty
                    JOptionPane.showMessageDialog(frmMandeHelper, "Both fields must be filled out.", "Validation Error", JOptionPane.ERROR_MESSAGE);

                } else {
                    // Proceed with further actions (e.g., submitting the data)
//                    JOptionPane.showMessageDialog(frmMandeHelper, "Validation successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
                   
                	// Confirm Pop Up
                	int a=JOptionPane.showConfirmDialog(frmMandeHelper,"Are you sure?");  
                	if(a==JOptionPane.YES_OPTION){  
                		frmMandeHelper.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                		CombineDoc();
                	  }  
                	
                }
			}
		});
		btnNewButton.setFont(new Font("Tahoma", Font.PLAIN, 11));
		btnNewButton.setBounds(223, 194, 89, 23);
		panel.add(btnNewButton);
		
		JButton btnNewButton_1 = new JButton("Cancel");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		btnNewButton_1.setFont(new Font("Tahoma", Font.PLAIN, 11));
		btnNewButton_1.setBounds(111, 194, 89, 23);
		panel.add(btnNewButton_1);
		
		JButton btnNewButton_3 = new JButton("Browse");
		btnNewButton_3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fileChooser.setDialogTitle("Select an TXT File");

                // Optional: Set a filter to display only .txt files
                fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("TXT Files", "txt"));

                int result = fileChooser.showOpenDialog(frmMandeHelper);
                if (result == JFileChooser.APPROVE_OPTION) {
                	// Get the selected file
                    myObj = fileChooser.getSelectedFile();
                    
                    // Debugging purposes
                    System.out.println("Selected file: " + myObj.getAbsolutePath());
                    
                    //Update text field
                    textField_1.setText(myObj.getAbsolutePath());
                }
			}
		});
		btnNewButton_3.setFont(new Font("Tahoma", Font.PLAIN, 11));
		btnNewButton_3.setBounds(312, 106, 89, 23);
		panel.add(btnNewButton_3);
		
		JButton btnNewButton_4 = new JButton("Browse");
		btnNewButton_4.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setDialogTitle("Select a Target Folder");

                int result = fileChooser.showOpenDialog(frmMandeHelper);
                if (result == JFileChooser.APPROVE_OPTION) {
                	// Get the downloadPath 
                	downloadPath = fileChooser.getSelectedFile();
                	
                	// For Debugging Purposes
                    System.out.println("Selected folder: " + downloadPath.getAbsolutePath());
                	
                    // Set the folder path in the text field
                    textField_2.setText(downloadPath.getAbsolutePath());
                }
			}
		});
		btnNewButton_4.setFont(new Font("Tahoma", Font.PLAIN, 11));
		btnNewButton_4.setBounds(312, 145, 89, 23);
		panel.add(btnNewButton_4);
	
	}
}
