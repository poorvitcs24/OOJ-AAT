import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class RailwaySystem1 {

    // Station list
    private static final String[] STATIONS = {"Bangalore", "Chennai", "Mumbai", "Hyderabad", "Delhi", "Kolkata"};

    // Coach configuration
    private static final String COACH_1 = "1AC";
    private static final String COACH_2 = "2AC";
    private static final String COACH_3 = "3AC";
    private static final String COACH_GEN = "GEN";

    private static RailwaySystem1 instance;

    private static final Map<String, Integer> COACH_SEAT_COUNT = new LinkedHashMap<>();
    private static final Map<String, Integer> COACH_PRICE = new LinkedHashMap<>();

    static {
        COACH_SEAT_COUNT.put(COACH_1, 10);
        COACH_SEAT_COUNT.put(COACH_2, 20);
        COACH_SEAT_COUNT.put(COACH_3, 30);
        COACH_SEAT_COUNT.put(COACH_GEN, 40);

        COACH_PRICE.put(COACH_1, 4000);
        COACH_PRICE.put(COACH_2, 2500);
        COACH_PRICE.put(COACH_3, 1000);
        COACH_PRICE.put(COACH_GEN, 500);
    }

    // Storage
    private final Map<String, Boolean> seatsAvailable = new HashMap<>();
    private final Map<String, Ticket> seatToTicket = new HashMap<>();
    private final List<Ticket> tickets = new ArrayList<>();
    private int ticketCounter = 1;
    private final Object bookingLock = new Object();

    // Main frame
    private JFrame mainFrame;

    public RailwaySystem1() {
        instance = this;
        initializeSeats();
        SwingUtilities.invokeLater(this::createAndShowGUI);
    }

    private void initializeSeats() {
        for (Map.Entry<String, Integer> e : COACH_SEAT_COUNT.entrySet()) {
            String coach = e.getKey();
            int count = e.getValue();
            for (int i = 1; i <= count; i++) {
                String seatId = (coach.equals(COACH_GEN) ? "GEN" : coach) + "-S" + i;
                seatsAvailable.put(seatId, true);
            }
        }
    }

    private void createAndShowGUI() {
        mainFrame = new JFrame("Railway Booking - Main Menu");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(520, 300);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("Railway Ticket Booking System", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainFrame.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new GridLayout(2, 2, 12, 12));
        center.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton bookBtn = new JButton("Book Tickets");
        JButton cancelBtn = new JButton("Cancel Ticket");
        JButton viewBtn = new JButton("View Tickets (Printable)");
        JButton resetBtn = new JButton("Reset All (DEV)");

        bookBtn.setFont(new Font("SansSerif", Font.PLAIN, 16));
        cancelBtn.setFont(new Font("SansSerif", Font.PLAIN, 16));
        viewBtn.setFont(new Font("SansSerif", Font.PLAIN, 16));
        resetBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));

        center.add(bookBtn);
        center.add(cancelBtn);
        center.add(viewBtn);
        center.add(resetBtn);

        mainFrame.add(center, BorderLayout.CENTER);

        JLabel footer = new JLabel(
                "Coaches: 1AC(10), 2AC(20), 3AC(30), General(40) | Age<=15 => Half Fare",
                SwingConstants.CENTER);
        footer.setBorder(new EmptyBorder(0, 0, 10, 0));
        mainFrame.add(footer, BorderLayout.SOUTH);

        bookBtn.addActionListener(e -> SwingUtilities.invokeLater(this::openBookingWindow));
        cancelBtn.addActionListener(e -> SwingUtilities.invokeLater(this::openCancelWindow));
        viewBtn.addActionListener(e -> SwingUtilities.invokeLater(this::openViewTicketsWindow));
        resetBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(mainFrame,
                    "Reset clears ALL bookings. Continue?", "Confirm Reset", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                synchronized (bookingLock) {
                    seatsAvailable.keySet().forEach(k -> seatsAvailable.put(k, true));
                    seatToTicket.clear();
                    tickets.clear();
                    ticketCounter = 1;
                }
                JOptionPane.showMessageDialog(mainFrame, "All bookings cleared.", "Reset Done",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        mainFrame.setVisible(true);
    }

    // ---------- Booking Window ----------
    private void openBookingWindow() {
        JDialog bookDialog = new JDialog(mainFrame, "Book Tickets", true);
        bookDialog.setSize(900, 600);
        bookDialog.setLocationRelativeTo(mainFrame);
        bookDialog.setLayout(new BorderLayout(10, 10));

        // Top panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        topPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        topPanel.add(new JLabel("Coach:"));
        JComboBox<String> coachCombo = new JComboBox<>(COACH_SEAT_COUNT.keySet().toArray(new String[0]));
        topPanel.add(coachCombo);

        topPanel.add(new JLabel("From:"));
        JComboBox<String> fromCombo = new JComboBox<>(STATIONS);
        topPanel.add(fromCombo);

        topPanel.add(new JLabel("To:"));
        JComboBox<String> toCombo = new JComboBox<>(STATIONS);
        toCombo.setSelectedIndex(1);
        topPanel.add(toCombo);

        JLabel availLabel = new JLabel("Available seats: 0");
        topPanel.add(availLabel);
        bookDialog.add(topPanel, BorderLayout.NORTH);

        // Center panel
        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel seatsContainer = new JPanel();
        seatsContainer.setLayout(new BorderLayout());
        JScrollPane seatsScroll = new JScrollPane(seatsContainer, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        centerPanel.add(seatsScroll, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        rightPanel.setPreferredSize(new Dimension(300, 300));

        JLabel instruction = new JLabel(
                "<html><b>Instructions:</b><br>1) Choose coach.<br>2) Manually select one or more seats.<br>3) Click 'Proceed' to enter passenger details for each seat.<br>4) Each seat becomes a separate ticket.</html>");
        instruction.setBorder(new EmptyBorder(0, 0, 10, 0));
        rightPanel.add(instruction);

        JButton proceedBtn = new JButton("Proceed (Enter passenger data & Book)");
        proceedBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        rightPanel.add(proceedBtn);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        JLabel priceNote = new JLabel("Prices: 1AC=4000 | 2AC=2500 | 3AC=1000 | GEN=500");
        priceNote.setFont(new Font("SansSerif", Font.PLAIN, 12));
        rightPanel.add(priceNote);
        centerPanel.add(rightPanel, BorderLayout.EAST);
        bookDialog.add(centerPanel, BorderLayout.CENTER);

        // Bottom panel
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeBtn = new JButton("Close");
        bottom.add(closeBtn);
        bookDialog.add(bottom, BorderLayout.SOUTH);

        // Build seat checkboxes
        final Map<String, JCheckBox> currentSeatCheckboxes = new LinkedHashMap<>();
        Runnable buildSeatsUI = () -> {
            seatsContainer.removeAll();
            currentSeatCheckboxes.clear();
            String coach = (String) coachCombo.getSelectedItem();
            if (coach == null)
                return;
            int seatCount = COACH_SEAT_COUNT.getOrDefault(coach, 0);
            JPanel grid = new JPanel(new GridLayout(Math.max(3, (seatCount + 4) / 5), 5, 8, 8));
            grid.setBorder(new EmptyBorder(8, 8, 8, 8));
            int available = 0;
            for (int i = 1; i <= seatCount; i++) {
                String seatId = (coach.equals(COACH_GEN) ? "GEN" : coach) + "-S" + i;
                JCheckBox cb = new JCheckBox(seatId);
                boolean avail = seatsAvailable.getOrDefault(seatId, true);
                cb.setEnabled(avail);
                if (!avail)
                    cb.setText(seatId + " (Booked)");
                if (avail)
                    available++;
                currentSeatCheckboxes.put(seatId, cb);
                grid.add(cb);
            }
            seatsContainer.add(grid, BorderLayout.CENTER);
            seatsContainer.revalidate();
            seatsContainer.repaint();
            availLabel.setText("Available seats: " + available);
        };

        buildSeatsUI.run();
        coachCombo.addActionListener(e -> buildSeatsUI.run());

        proceedBtn.addActionListener(e -> {
            String coach = (String) coachCombo.getSelectedItem();
            String from = (String) fromCombo.getSelectedItem();
            String to = (String) toCombo.getSelectedItem();
            if (from == null || to == null || from.equals(to)) {
                JOptionPane.showMessageDialog(bookDialog, "Please select different 'From' and 'To' stations.",
                        "Invalid Stations", JOptionPane.ERROR_MESSAGE);
                return;
            }
            List<String> selectedSeats = currentSeatCheckboxes.entrySet().stream()
                    .filter(entry -> entry.getValue().isSelected() && entry.getValue().isEnabled())
                    .map(Map.Entry::getKey).collect(Collectors.toList());
            if (selectedSeats.isEmpty()) {
                JOptionPane.showMessageDialog(bookDialog, "No seats selected. Please select seat(s) to book.",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            double totalCost = 0;
            int bookedCount = 0;
            for (String seatId : selectedSeats) {
                PassengerDialog pd = new PassengerDialog(bookDialog, seatId);
                pd.setVisible(true);
                if (!pd.isConfirmed())
                    continue;
                String pname = pd.getPassengerName();
                int age = pd.getPassengerAge();
                int basePrice = COACH_PRICE.getOrDefault(coach, 500);
                double price = age <= 15 ? basePrice / 2.0 : basePrice;
                String ticketId = generateTicketId();
                Ticket t = new Ticket(ticketId, pname, age, seatId, coach, from, to, price, new Date());
                synchronized (bookingLock) {
                    tickets.add(t);
                    seatToTicket.put(seatId, t);
                    seatsAvailable.put(seatId, false);
                }

                JCheckBox cb = currentSeatCheckboxes.get(seatId);
                if (cb != null) {
                    cb.setEnabled(false);
                    cb.setSelected(false);
                    cb.setText(seatId + " (Booked)");
                }
                totalCost += price;
                bookedCount++;
            }
            buildSeatsUI.run();
            if (bookedCount > 0) {
                JOptionPane.showMessageDialog(bookDialog, bookedCount + " ticket(s) booked. Total: Rs. "
                        + String.format("%.2f", totalCost), "Booking Successful", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(bookDialog,
                        "No tickets were booked (maybe you cancelled some passenger dialogs).",
                        "Booking Cancelled", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        closeBtn.addActionListener(e -> bookDialog.dispose());
        bookDialog.setVisible(true);
    }

    // ---------- Passenger Dialog ----------
    private static class PassengerDialog extends JDialog {
        private boolean confirmed = false;
        private final JTextField nameField;
        private final JTextField ageField;

        PassengerDialog(Window parent, String seatId) {
            super(parent, "Passenger for " + seatId, ModalityType.APPLICATION_MODAL);
            setSize(360, 220);
            setLocationRelativeTo(parent);
            setLayout(null);

            JLabel info = new JLabel("Enter passenger details for " + seatId);
            info.setBounds(16, 10, 320, 24);
            add(info);

            JLabel nameLbl = new JLabel("Name:");
            nameLbl.setBounds(16, 48, 80, 24);
            add(nameLbl);
            nameField = new JTextField();
            nameField.setBounds(100, 48, 220, 24);
            add(nameField);

            JLabel ageLbl = new JLabel("Age:");
            ageLbl.setBounds(16, 88, 80, 24);
            add(ageLbl);
            ageField = new JTextField();
            ageField.setBounds(100, 88, 80, 24);
            add(ageField);

            JButton ok = new JButton("OK");
            ok.setBounds(60, 130, 100, 30);
            add(ok);
            JButton cancel = new JButton("Cancel");
            cancel.setBounds(190, 130, 100, 30);
            add(cancel);

            ok.addActionListener(e -> {
                String name = nameField.getText().trim();
                String ageText = ageField.getText().trim();
                if (name.isEmpty() || ageText.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please fill both name and age.", "Input Required",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int age;
                try {
                    age = Integer.parseInt(ageText);
                    if (age < 0 || age > 120) {
                        JOptionPane.showMessageDialog(this, "Enter a valid age (0-120).", "Invalid Age",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Age must be a number.", "Invalid Input",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                confirmed = true;
                setVisible(false);
            });

            cancel.addActionListener(e -> {
                confirmed = false;
                setVisible(false);
            });
        }

        boolean isConfirmed() {
            return confirmed;
        }

        String getPassengerName() {
            return nameField.getText().trim();
        }

        int getPassengerAge() {
            try {
                return Integer.parseInt(ageField.getText().trim());
            } catch (Exception e) {
                return 0;
            }
        }
    }

    // ---------- Cancel Ticket ----------
    private void openCancelWindow() {
        JDialog cancelDialog = new JDialog(mainFrame, "Cancel Ticket", true);
        cancelDialog.setSize(420, 220);
        cancelDialog.setLocationRelativeTo(mainFrame);
        cancelDialog.setLayout(new BorderLayout(8, 8));
        cancelDialog.setResizable(false);

        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel lbl = new JLabel("Enter seat ID to cancel (e.g., 1AC-S3 or GEN-S12):");
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        main.add(lbl);

        JTextField seatField = new JTextField();
        seatField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        seatField.setAlignmentX(Component.LEFT_ALIGNMENT);
        main.add(Box.createRigidArea(new Dimension(0, 8)));
        main.add(seatField);
        main.add(Box.createRigidArea(new Dimension(0, 12)));

        JButton cancelBtn = new JButton("Cancel Ticket");
        cancelBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        main.add(cancelBtn);
        main.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel note = new JLabel("<html><i>Note:</i> Seat IDs are coach-prefixed. Example: 2AC-S5, GEN-S12. Use exact ID.</html>");
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        main.add(note);

        cancelDialog.add(main, BorderLayout.CENTER);

        cancelBtn.addActionListener(e -> {
            String seatInput = seatField.getText().trim().toUpperCase();
            if (seatInput.isEmpty()) {
                JOptionPane.showMessageDialog(cancelDialog, "Please enter a seat ID.", "Input Required",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!seatsAvailable.containsKey(seatInput)) {
                JOptionPane.showMessageDialog(cancelDialog, "Seat ID not recognized: " + seatInput, "Invalid Seat",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            Ticket t = seatToTicket.get(seatInput);
            if (t == null) {
                JOptionPane.showMessageDialog(cancelDialog, "No booking found for " + seatInput, "Not Found",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            int conf = JOptionPane.showConfirmDialog(cancelDialog,
                    "Cancel ticket " + t.ticketId + " for " + t.name + " (Seat " + seatInput + ")?", "Confirm Cancel",
                    JOptionPane.YES_NO_OPTION);
            if (conf != JOptionPane.YES_OPTION)
                return;

            synchronized (bookingLock) {
                tickets.remove(t);
                seatToTicket.remove(seatInput);
                seatsAvailable.put(seatInput, true);
            }

            JOptionPane.showMessageDialog(cancelDialog, "Ticket cancelled. Seat " + seatInput + " is now available.",
                    "Cancelled", JOptionPane.INFORMATION_MESSAGE);
            seatField.setText("");
        });

        cancelDialog.setVisible(true);
    }

    private static abstract class BaseTicket {
        String ticketId;
        String name;
        int age;
        String seat;
        String coach;
        String from;
        String to;
        double price;
        Date bookedOn;

        BaseTicket(String ticketId, String name, int age, String seat, String coach,String from, String to, double price, Date bookedOn) {
            this.ticketId = ticketId;
            this.name = name;
            this.age = age;
            this.seat = seat;
            this.coach = coach;
            this.from = from;
            this.to = to;
            this.price = price;
            this.bookedOn = bookedOn;
        }

        // Polymorphic method
        abstract String getTicketType();
    }

    // ---------- Ticket class ----------
    private static class Ticket extends BaseTicket {

        Ticket(String ticketId, String name, int age,
            String seat, String coach,
            String from, String to,
            double price, Date bookedOn) {

            super(ticketId, name, age, seat, coach, from, to, price, bookedOn);
        }

        @Override
        String getTicketType() {
            return "Regular Ticket";
        }
    }

    // ---------- TicketPanel (with Ticket) ----------
    private class TicketPanel extends JPanel {
        private Image bgImg;
        private final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        private Ticket ticket;

        TicketPanel(Ticket t) {
            this.ticket = t;
            try {
                bgImg = Toolkit.getDefaultToolkit().getImage("train_bg.png"); // optional background image
            } catch (Exception ignored) {}
            setPreferredSize(new Dimension(2000, 180)); // broader width
            setMaximumSize(new Dimension(2000, 180));  // keep consistent width in BoxLayout
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (ticket == null) return;

            Graphics2D g2 = (Graphics2D) g;

            // Yellow gradient block
            Color topColor = new Color(255, 245, 180);
            Color bottomColor = new Color(255, 255, 210);
            GradientPaint gp = new GradientPaint(0, 0, topColor, 0, getHeight(), bottomColor);
            g2.setPaint(gp);
            g2.fillRoundRect(5, 5, getWidth() - 10, getHeight() - 10, 20, 20);

            // Optional train image
            if (bgImg != null) {
                g2.setClip(new Rectangle(5, 5, getWidth() - 10, getHeight() - 10));
                g2.drawImage(bgImg, 5, 5, getWidth() - 10, getHeight() - 10, this);
                g2.setClip(null);
            }

            // Draw ticket details - left aligned with padding
            g2.setColor(Color.BLACK);
            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            int paddingX = 20; // horizontal padding from left
            int paddingY = 15; // vertical padding from top
            int lineSpacing = 12; // spacing between lines

            int y = paddingY;
            g2.drawString("Ticket ID : " + ticket.ticketId, paddingX, y);
            y += lineSpacing;
            g2.drawString("Type      : " + ticket.getTicketType(), paddingX, y);
            y += lineSpacing;
            g2.drawString("Name      : " + ticket.name, paddingX, y);
            y += lineSpacing;
            g2.drawString("Age       : " + ticket.age, paddingX, y);
            y += lineSpacing;
            g2.drawString("Coach     : " + ticket.coach, paddingX, y);
            y += lineSpacing;
            g2.drawString("Seat      : " + ticket.seat, paddingX, y);
            y += lineSpacing;
            g2.drawString("From      : " + ticket.from, paddingX, y);
            y += lineSpacing;
            g2.drawString("To        : " + ticket.to, paddingX, y);
            y += lineSpacing;
            g2.drawString("Price     : Rs. " + ticket.price, paddingX, y);
            y += lineSpacing;
            g2.drawString("Booked On : " + sdf.format(ticket.bookedOn), paddingX, y);
        }
    }

    // ---------- View Tickets ----------
    private void openViewTicketsWindow() {
        JDialog viewDialog = new JDialog(mainFrame, "View Tickets (Printable)", true);
        viewDialog.setSize(700, 600);
        viewDialog.setLocationRelativeTo(mainFrame);
        viewDialog.setLayout(new BorderLayout(8, 8));

        JPanel ticketsPanel = new JPanel();
        ticketsPanel.setLayout(new BoxLayout(ticketsPanel, BoxLayout.Y_AXIS));
        ticketsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        loadTicketPanels(ticketsPanel);

        JScrollPane scroll = new JScrollPane(ticketsPanel);
        viewDialog.add(scroll, BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> {
            ticketsPanel.removeAll();
            loadTicketPanels(ticketsPanel);
            ticketsPanel.revalidate();
            ticketsPanel.repaint();
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        top.add(refreshBtn);
        viewDialog.add(top, BorderLayout.NORTH);

        viewDialog.setVisible(true);
    }

    private void loadTicketPanels(JPanel ticketsPanel) {
        synchronized (bookingLock) {
            for (Ticket t : tickets) {
                ticketsPanel.add(Box.createVerticalStrut(10));
                JLabel top = new JLabel("------------------------------------------------------------");
                top.setAlignmentX(Component.CENTER_ALIGNMENT);
                ticketsPanel.add(top);
                ticketsPanel.add(Box.createVerticalStrut(5));
                TicketPanel tp = new TicketPanel(t);
                tp.setPreferredSize(new Dimension(350, 130));
                tp.setMaximumSize(new Dimension(350, 130));
                tp.setAlignmentX(Component.CENTER_ALIGNMENT);
                ticketsPanel.add(tp);
                ticketsPanel.add(Box.createVerticalStrut(5));
                JLabel bottom = new JLabel("------------------------------------------------------------");
                bottom.setAlignmentX(Component.CENTER_ALIGNMENT);
                ticketsPanel.add(bottom);
                ticketsPanel.add(Box.createVerticalStrut(15));
            }
        }
    }

    // ---------- Helpers ----------
    private String generateTicketId() {
        synchronized (bookingLock) {
            return String.format("T%03d", ticketCounter++);
        }
    }

    public static List<Ticket> ticketsStatic() {
        return instance.tickets;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        new RailwaySystem1();
    }
}