import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 *
 * @author Emil
 */
public class FXMLDocumentController implements Initializable {

    @FXML
    private TextArea lcTextArea;
    @FXML
    private TextArea plcTextArea;
    @FXML
    private TextArea lcsTextArea;
    @FXML
    private TextArea plcsTextArea;
    @FXML
    private TextArea reTextArea;
    @FXML
    private Spinner amountSpinner;
    @FXML
    private TextField tfComponent;
    @FXML
    private TextField tfCS;
    @FXML
    private TextField tfTotal;

    private Connection con = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        amountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 99999));
        String database = "jdbc:postgresql://localhost:5432/computerStore";
        String user = "postgres";
        String password = "1234";
        try {
            con = DriverManager.getConnection(database, user, password);
        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(FXMLDocumentController.class.getName());
            lgr.log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    @FXML
    private void listComponents(ActionEvent event) {
        components(con);
    }

    @FXML
    private void pricelestComponents(ActionEvent event) {
        priceListComponents(con);
    }

    @FXML
    private void listCS(ActionEvent event) {
        computerSystems(con);
    }

    @FXML
    private void pricelistCS(ActionEvent event) {
        priceListCS(con);
    }

    @FXML
    private void buyComponent(ActionEvent event) {
        sellComponent(con);
        update();
    }

    @FXML
    private void buyCS(ActionEvent event) {
        sellCS(con);
        update();
    }

    @FXML
    private void restockList(ActionEvent event) {
        restockingList(con);
    }

    @FXML
    private void restockToPref(ActionEvent event) {
        restockComponents(con);
        update();
    }

    private void components(Connection con) {
        try {
            Statement st = con.createStatement();
            String query = "SELECT name, currentamount FROM component ORDER BY name";
            ResultSet rs = st.executeQuery(query);
            String text = "";
            while (rs.next()) {
                text += (rs.getString(1) + " " + rs.getInt(2) + "\n");
            }
            lcTextArea.setText(text);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void computerSystems(Connection con) {
        try {
            Statement st = con.createStatement();
            String query = "SELECT cs.fancyname, min(component.currentamount)\n"
                    + "FROM cs, component\n"
                    + "WHERE component.name IN (cs.case, cs.mainboard, cs.cpu, cs.ram, cs.graphicscard)\n"
                    + "GROUP by cs.fancyname\n"
                    + "ORDER BY cs.fancyname";
            ResultSet rs = st.executeQuery(query);
            String text = "";
            while (rs.next()) {
                text += (rs.getString(1) + " " + rs.getInt(2) + "\n");
            }
            lcsTextArea.setText(text);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void priceListComponents(Connection con) {
        try {
            Statement st = con.createStatement();
            String query = "SELECT name, price, kind FROM component ORDER by kind";
            ResultSet rs = st.executeQuery(query);
            String text = "";
            while (rs.next()) {
                text += (rs.getString(3) + " " + rs.getString(1) + "" + String.format("%.2f", rs.getFloat(2) * 1.3) + "\n");
            }
            plcTextArea.setText(text);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void priceListCS(Connection con) {
        try {
            Statement st = con.createStatement();
            String query = "SELECT cs.fancyname,\n"
                    + "(SELECT component.name FROM component WHERE cs.case = component.name),\n"
                    + "(SELECT component.name FROM component WHERE cs.mainboard = component.name),\n"
                    + "(SELECT component.name FROM component WHERE cs.cpu = component.name),\n"
                    + "(SELECT component.name FROM component WHERE cs.ram = component.name),\n"
                    + "(SELECT component.name FROM component WHERE cs.graphicscard= component.name),\n"
                    + "(SELECT\n"
                    + "(SELECT price FROM component WHERE cs.case = component.name)+\n"
                    + "(SELECT price FROM component WHERE cs.mainboard = component.name)+\n"
                    + "(SELECT price FROM component WHERE cs.cpu = component.name)+\n"
                    + "(SELECT price FROM component WHERE cs.ram = component.name)+\n"
                    + "(SELECT COALESCE(SUM(price),0) FROM component WHERE cs.graphicscard = component.name) \n"
                    + "AS total)\n"
                    + "FROM cs ORDER BY fancyname";
            ResultSet rs = st.executeQuery(query);
            String text = "";
            while (rs.next()) {
                text += (rs.getString(1) + " " + ((int) (Math.round(rs.getDouble(7)) - (Math.round(rs.getDouble(7)) % 100)) + 99) + " | " + rs.getString(2) + " " + rs.getString(3) + " " + rs.getString(4) + " " + rs.getString(5) + " " + rs.getString(6) + "\n");
            }
            plcsTextArea.setText(text);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void sellComponent(Connection con) {
        try {
            Statement st = con.createStatement();
            String query = "UPDATE component\n"
                    + "SET currentamount = currentamount -1\n"
                    + "WHERE component.name ='" + tfComponent.getText() + "'";
            st.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void sellCS(Connection con) {
        try {
            Statement st = con.createStatement();
            String query = "UPDATE component\n"
                    + "SET currentamount=currentamount - " + amountSpinner.getValue() + "\n"
                    + "FROM cs\n"
                    + "WHERE cs.fancyname = '" + tfCS.getText() + "' \n"
                    + "AND component.name IN (cs.case, cs.mainboard, cs.cpu, cs.ram, cs.graphicscard)";
            st.executeUpdate(query);
            priceOffer(con);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void restockingList(Connection con) {
        try {
            Statement st = con.createStatement();
            String query = "SELECT name, SUM(prefferedamount-currentamount)\n"
                    + "FROM component\n"
                    + "WHERE currentamount < minimumamount\n"
                    + "GROUP BY name";
            ResultSet rs = st.executeQuery(query);
            String text = "";
            while (rs.next()) {
                text += (rs.getString(1) + " " + rs.getInt(2) + "\n");
            }
            reTextArea.setText(text);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void restockComponents(Connection con) {
        try {
            Statement st = con.createStatement();
            String query = "UPDATE component\n"
                    + "SET currentamount = prefferedamount\n"
                    + "WHERE currentamount < minimumamount";
            st.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void priceOffer(Connection con) {
        try {
            Statement st = con.createStatement();
            String query = "SELECT cs.fancyname,\n"
                    + "(SELECT \n"
                    + "(SELECT price FROM component WHERE cs.case = component.name)+\n"
                    + "(SELECT price FROM component WHERE cs.mainboard = component.name)+\n"
                    + "(SELECT price FROM component WHERE cs.cpu = component.name)+\n"
                    + "(SELECT price FROM component WHERE cs.ram = component.name)+\n"
                    + "(SELECT COALESCE(SUM(price),0) FROM component WHERE cs.graphicscard = component.name))\n"
                    + "FROM cs WHERE cs.fancyname = '" + tfCS.getText() + "'";
            ResultSet rs = st.executeQuery(query);
            double total = 0;
            while (rs.next()) {
                total = (Math.round(rs.getDouble(2)) - (Math.round(rs.getDouble(2)) % 100)) + 99;
            }
            int amount = (Integer) amountSpinner.getValue();
            if (amount == 0) {
                tfTotal.setText("0");
            } else if (amount == 1) {
                tfTotal.setText(String.valueOf((int) total));
            } else if (amount <= 10) {
                tfTotal.setText(String.valueOf((int) (total - total * ((amount - 1) * 0.02)) * amount));
            } else {
                tfTotal.setText(String.valueOf((int) (total - total * (0.2)) * amount));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void update() {
        components(con);
        computerSystems(con);
        restockingList(con);
    }
}