
import java.sql.*;
import java.util.Scanner;

public class Main {

    static Scanner sc = new Scanner(System.in);

    static String url = "jdbc:mysql://localhost:3306/hostel_db";
    static String user = "root";
    static String password = "admin";

    public static void main(String[] args) {

        try (Connection con =
                     DriverManager.getConnection(url, user, password)) {

            System.out.println("Database Connected Successfully!");

            int choice;

            do {
                System.out.println("\n===== KIET HOSTEL MANAGEMENT SYSTEM =====");
                System.out.println("1. Insert Student Records");
                System.out.println("2. Display Students by Hostel");
                System.out.println("3. Update Hostel Fee");
                System.out.println("4. Search and Update Room Number");
                System.out.println("5. Display Total Fee and Delete Student");
                System.out.println("6. Exit");
                System.out.print("Enter your choice: ");

                choice = Integer.parseInt(sc.nextLine());

                switch (choice) {

                    case 1:
                        insertStudents(con);
                        break;

                    case 2:
                        searchStudents(con);
                        break;

                    case 3:
                        updateFee(con);
                        break;

                    case 4:
                        updateRoom(con);
                        break;

                    case 5:
                        totalFeeAndDelete(con);
                        break;

                    case 6:
                        System.out.println("Thank you! Program closed.");
                        break;

                    default:
                        System.out.println("Invalid choice. Try again.");
                }

            } while (choice != 6);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // (a) Insert three student records
    static void insertStudents(Connection con) throws SQLException {

        String sql = "INSERT INTO HostelStudent " +
                "(student_id, student_name, roll_number, hostel_name, room_number, fee) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        int[] ids = {101, 102, 103};

        String[] names = {
                "Rahul Sharma", "Aman Verma", "Priya Singh"
        };

        String[] rolls = {"230001", "230002", "230003"};

        String[] hostels = {
                "Boys Hostel A", "Boys Hostel B", "Girls Hostel A"
        };

        String[] rooms = {"101", "202", "303"};

        double[] fees = {65000, 55000, 70000};

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            for (int i = 0; i < 3; i++) {

                ps.setInt(1, ids[i]);
                ps.setString(2, names[i]);
                ps.setString(3, rolls[i]);
                ps.setString(4, hostels[i]);
                ps.setString(5, rooms[i]);
                ps.setDouble(6, fees[i]);

                try {
                    int rows = ps.executeUpdate();

                    if (rows > 0) {
                        System.out.println(names[i] + " inserted successfully.");
                    }

                } catch (SQLIntegrityConstraintViolationException e) {
                    System.out.println("Duplicate Student ID: " + ids[i]);
                }
            }
        }
    }

    // (b) Display students by hostel with fee > 50000
    static void searchStudents(Connection con) throws SQLException {

        System.out.print("Enter hostel name: ");
        String hostel = sc.nextLine();

        String sql = "SELECT * FROM HostelStudent " +
                "WHERE hostel_name = ? AND fee > 50000 " +
                "ORDER BY fee DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, hostel);

            try (ResultSet rs = ps.executeQuery()) {

                boolean found = false;

                while (rs.next()) {
                    found = true;
                    displayStudent(rs);
                }

                if (!found) {
                    System.out.println("No matching students found.");
                }
            }
        }
    }

    // (c) Update hostel fee
    static void updateFee(Connection con) throws SQLException {

        System.out.print("Enter student ID: ");
        int id = Integer.parseInt(sc.nextLine());

        System.out.print("Enter new hostel fee: ");
        double fee = Double.parseDouble(sc.nextLine());

        String sql =
                "UPDATE HostelStudent SET fee = ? WHERE student_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDouble(1, fee);
            ps.setInt(2, id);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Hostel fee updated successfully.");
                showStudentById(con, id);
            } else {
                if (studentExists(con, id)) {
                    System.out.println("Fee is already the same. No change needed.");
                } else {
                    System.out.println("Student does not exist.");
                }
            }
        }
    }

    // (d) Search and update room number
    static void updateRoom(Connection con) throws SQLException {

        System.out.print("Enter student ID: ");
        int id = Integer.parseInt(sc.nextLine());

        if (!studentExists(con, id)) {
            System.out.println("Student does not exist.");
            return;
        }

        System.out.println("Current Student Details:");
        showStudentById(con, id);

        System.out.print("Enter new room number: ");
        String room = sc.nextLine();

        String sql =
                "UPDATE HostelStudent SET room_number = ? WHERE student_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, room);
            ps.setInt(2, id);

            ps.executeUpdate();

            System.out.println("Room number updated successfully.");
            System.out.println("Updated Student Details:");
            showStudentById(con, id);
        }
    }

    // (e) Display total fee and delete student if fee > 0
    static void totalFeeAndDelete(Connection con) throws SQLException {

        String sumSQL =
                "SELECT COALESCE(SUM(fee), 0) AS total_fee FROM HostelStudent";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sumSQL)) {

            if (rs.next()) {
                System.out.println("Total Hostel Fee: " +
                        rs.getDouble("total_fee"));
            }
        }

        System.out.print("Enter student ID to delete: ");
        int id = Integer.parseInt(sc.nextLine());

        // First check whether student exists and fee is positive
        String checkSQL =
                "SELECT fee FROM HostelStudent WHERE student_id = ?";

        try (PreparedStatement check = con.prepareStatement(checkSQL)) {

            check.setInt(1, id);

            try (ResultSet rs = check.executeQuery()) {

                if (!rs.next()) {
                    System.out.println("Student does not exist.");
                    return;
                }

                double fee = rs.getDouble("fee");

                if (fee <= 0) {
                    System.out.println(
                            "Student cannot be deleted because fee is not greater than 0.");
                    return;
                }
            }
        }

        // Delete the student
        String deleteSQL =
                "DELETE FROM HostelStudent WHERE student_id = ? AND fee > 0";

        try (PreparedStatement ps = con.prepareStatement(deleteSQL)) {

            ps.setInt(1, id);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Student deleted successfully!");
            } else {
                System.out.println("Student was not deleted.");
            }
        }

        // Display remaining records
        System.out.println("\nRemaining Student Records:");

        String displaySQL = "SELECT * FROM HostelStudent";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(displaySQL)) {

            boolean found = false;

            while (rs.next()) {
                found = true;
                displayStudent(rs);
            }

            if (!found) {
                System.out.println("No student records available.");
            }
        }
    }

    // Check student existence
    static boolean studentExists(Connection con, int id)
            throws SQLException {

        String sql =
                "SELECT student_id FROM HostelStudent WHERE student_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Display student by ID
    static void showStudentById(Connection con, int id)
            throws SQLException {

        String sql =
                "SELECT * FROM HostelStudent WHERE student_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    displayStudent(rs);
                }
            }
        }
    }

    // Display student details
    static void displayStudent(ResultSet rs) throws SQLException {

        System.out.println(
                "ID: " + rs.getInt("student_id") +
                        ", Name: " + rs.getString("student_name") +
                        ", Roll Number: " + rs.getString("roll_number") +
                        ", Hostel: " + rs.getString("hostel_name") +
                        ", Room: " + rs.getString("room_number") +
                        ", Fee: " + rs.getDouble("fee")
        );
    }
}
