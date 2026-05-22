module com.example.infix_to_postfix {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.infix_to_postfix to javafx.fxml;
    exports com.example.infix_to_postfix;
}