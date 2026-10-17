module ni.edu.uam.actividadpracticaevaluada {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens ni.edu.uam.actividadpracticaevaluada.controller to javafx.fxml;
    opens ni.edu.uam.actividadpracticaevaluada.model to javafx.fxml;
    opens ni.edu.uam.actividadpracticaevaluada to javafx.fxml;
    exports ni.edu.uam.actividadpracticaevaluada;
    exports ni.edu.uam.actividadpracticaevaluada.controller;
    exports ni.edu.uam.actividadpracticaevaluada.model;
}