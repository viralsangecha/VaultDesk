package com.vaultdesk.admin;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.http.*;
import java.util.Optional;

public class EmployeeView {

    public VBox getView() {
        Label title = new Label("Employees");
        title.getStyleClass().add("section-title");
        TableView<Employee> table = new TableView<>();

        TableColumn<Employee, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());

        TableColumn<Employee, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Employee, String> empCodeCol = new TableColumn<>("Emp Code");
        empCodeCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmpCode()));

        TableColumn<Employee, String> designationCol = new TableColumn<>("Designation");
        designationCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDesignation()));

        TableColumn<Employee, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmail()));

        TableColumn<Employee, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPhone()));

        TableColumn<Employee, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deactivateBtn = new Button("Deactivate");
            {
                editBtn.getStyleClass().add("btn-warning");
                editBtn.setOnAction(e -> {
                    Employee emp = getTableView().getItems().get(getIndex());
                    showEditDialog(emp, getTableView());
                });
                deactivateBtn.getStyleClass().add("btn-danger");
                deactivateBtn.setOnAction(e -> {
                    Employee emp = getTableView().getItems().get(getIndex());
                    showDeactivateConfirm(emp, getTableView());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(5, editBtn, deactivateBtn));
            }
        });

        table.getColumns().addAll(idCol, nameCol, empCodeCol,
                designationCol, emailCol, phoneCol, actionCol);

        Button addBtn = new Button("Add Employee");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> showAddDialog(table));

        TextField searchField = new TextField();
        searchField.setPromptText("Search by name...");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterTable(table, newVal);
        });

        HBox topBar = new HBox(10);
        topBar.getChildren().addAll(addBtn, searchField);

        loadEmployees(table);

        VBox root = new VBox(10);
        root.getChildren().addAll(title, topBar, table);
        return root;
    }

    private void filterTable(TableView<Employee> table, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            loadEmployees(table);
            return;
        }
        String lower = keyword.toLowerCase();
        table.getItems().removeIf(e ->
                !e.getName().toLowerCase().contains(lower));
    }

    private void loadEmployees(TableView<Employee> table) {
        table.getItems().clear();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/employees"))
                    .GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            String body = response.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    table.getItems().add(new Employee(
                            extractInt(obj, "id"),
                            extractValue(obj, "name"),
                            extractValue(obj, "empCode"),
                            extractValue(obj, "designation"),
                            extractValue(obj, "email"),
                            extractValue(obj, "phone")
                    ));
                }
            }
        } catch (Exception ex) {
            System.out.println("Error loading employees: " + ex.getMessage());
        }
    }

    private void showAddDialog(TableView<Employee> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Employee");
        dialog.setHeaderText("Enter employee details");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField();
        TextField empCodeField = new TextField();
        TextField deptField = new TextField();
        TextField designationField = new TextField();
        TextField emailField = new TextField();
        TextField phoneField = new TextField();
        TextField joinDateField = new TextField();
        joinDateField.setPromptText("YYYY-MM-DD");
        TextField notesField = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Name:"),        0, 0); grid.add(nameField,        1, 0);
        grid.add(new Label("Emp Code:"),    0, 1); grid.add(empCodeField,     1, 1);
        grid.add(new Label("Dept ID:"),     0, 2); grid.add(deptField,        1, 2);
        grid.add(new Label("Designation:"), 0, 3); grid.add(designationField, 1, 3);
        grid.add(new Label("Email:"),       0, 4); grid.add(emailField,       1, 4);
        grid.add(new Label("Phone:"),       0, 5); grid.add(phoneField,       1, 5);
        grid.add(new Label("Join Date:"),   0, 6); grid.add(joinDateField,    1, 6);
        grid.add(new Label("Notes:"),       0, 7); grid.add(notesField,       1, 7);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int deptId = deptField.getText().trim().isEmpty() ? 0
                        : Integer.parseInt(deptField.getText().trim());
                String body = "{" +
                        "\"name\":\"" + nameField.getText() + "\"," +
                        "\"empCode\":\"" + empCodeField.getText() + "\"," +
                        "\"departmentId\":" + deptId + "," +
                        "\"designation\":\"" + designationField.getText() + "\"," +
                        "\"email\":\"" + emailField.getText() + "\"," +
                        "\"phone\":\"" + phoneField.getText() + "\"," +
                        "\"joinDate\":\"" + joinDateField.getText() + "\"," +
                        "\"leaveDate\":null," +
                        "\"active\":1," +
                        "\"notes\":\"" + notesField.getText() + "\"" +
                        "}";
                if (postRequest("http://localhost:8080/api/employees", body, 201))
                    loadEmployees(table);
            } catch (NumberFormatException ex) {
                showAlert("Error", "Dept ID must be a number.");
            }
        }
    }

    private void showEditDialog(Employee emp, TableView<Employee> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Employee");
        dialog.setHeaderText("Editing: " + emp.getName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField(emp.getName());
        TextField designationField = new TextField(emp.getDesignation());
        TextField emailField = new TextField(emp.getEmail());
        TextField phoneField = new TextField(emp.getPhone());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Name:"),        0, 0); grid.add(nameField,        1, 0);
        grid.add(new Label("Designation:"), 0, 1); grid.add(designationField, 1, 1);
        grid.add(new Label("Email:"),       0, 2); grid.add(emailField,       1, 2);
        grid.add(new Label("Phone:"),       0, 3); grid.add(phoneField,       1, 3);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String body = "{" +
                    "\"id\":" + emp.getId() + "," +
                    "\"name\":\"" + nameField.getText() + "\"," +
                    "\"empCode\":\"" + emp.getEmpCode() + "\"," +
                    "\"departmentId\":0," +
                    "\"designation\":\"" + designationField.getText() + "\"," +
                    "\"email\":\"" + emailField.getText() + "\"," +
                    "\"phone\":\"" + phoneField.getText() + "\"," +
                    "\"joinDate\":null," +
                    "\"leaveDate\":null," +
                    "\"active\":1," +
                    "\"notes\":\"\"" +
                    "}";
            if (putRequest("http://localhost:8080/api/employees/" + emp.getId(), body))
                loadEmployees(table);
        }
    }

    private void showDeactivateConfirm(Employee emp, TableView<Employee> table) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Deactivate Employee");
        confirm.setHeaderText(null);
        confirm.setContentText("Deactivate " + emp.getName() + "?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (deleteRequest("http://localhost:8080/api/employees/" + emp.getId()))
                loadEmployees(table);
        }
    }

    private boolean postRequest(String url, String body, int expectedStatus) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == expectedStatus) {
                showAlert("Success", "Done successfully.");
                return true;
            }
            showAlert("Error", "Server returned: " + response.statusCode());
            return false;
        } catch (Exception ex) {
            showAlert("Error", "Cannot connect: " + ex.getMessage());
            return false;
        }
    }

    private boolean putRequest(String url, String body) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                showAlert("Success", "Updated successfully.");
                return true;
            }
            showAlert("Error", "Server returned: " + response.statusCode());
            return false;
        } catch (Exception ex) {
            showAlert("Error", "Cannot connect: " + ex.getMessage());
            return false;
        }
    }

    private boolean deleteRequest(String url) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                showAlert("Success", "Employee deactivated.");
                return true;
            }
            showAlert("Error", "Server returned: " + response.statusCode());
            return false;
        } catch (Exception ex) {
            showAlert("Error", "Cannot connect: " + ex.getMessage());
            return false;
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String extractValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private int extractInt(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.length();
        try {
            return Integer.parseInt(json.substring(start, end).trim().replace("}", ""));
        } catch (NumberFormatException e) { return 0; }
    }
}