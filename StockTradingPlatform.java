import java.io.*;
import java.util.*;

/*
 Simple single-file Stock Trading Platform simulation.
 Save as: StockTradingPlatform.java
 Compile: javac StockTradingPlatform.java
 Run:     java StockTradingPlatform
*/

public class StockTradingPlatform {
    // --- Domain classes as static inner classes to keep single-file simplicity ---
    static class Stock {
        String symbol;
        String name;
        double price; // current market price

        Stock(String symbol, String name, double price) {
            this.symbol = symbol;
            this.name = name;
            this.price = price;
        }

        @Override
        public String toString() {
            return symbol + " (" + name + ") - " + String.format("%.2f", price);
        }
    }

    static class Holding {
        String symbol;
        int shares;
        double avgPrice; // average buy price

        Holding(String symbol, int shares, double avgPrice) {
            this.symbol = symbol;
            this.shares = shares;
            this.avgPrice = avgPrice;
        }

        double marketValue(double marketPrice) {
            return shares * marketPrice;
        }

        @Override
        public String toString() {
            return symbol + ": " + shares + " shares @ avg " + String.format("%.2f", avgPrice);
        }
    }

    static class Transaction {
        Date time;
        String type; // BUY or SELL
        String symbol;
        int shares;
        double price; // price per share

        Transaction(String type, String symbol, int shares, double price) {
            this.time = new Date();
            this.type = type;
            this.symbol = symbol;
            this.shares = shares;
            this.price = price;
        }

        @Override
        public String toString() {
            return String.format("%tF %tT - %s %d %s @ %.2f", time, time, type, shares, symbol, price);
        }
    }

    static class Market {
        private final Map<String, Stock> stocks = new HashMap<>();
        private final Random rnd = new Random();

        Market() {
            // seed with some example stocks and initial prices
            addStock(new Stock("AAPL", "Apple Inc.", 172.45));
            addStock(new Stock("GOOG", "Alphabet Inc.", 128.30));
            addStock(new Stock("AMZN", "Amazon.com", 149.10));
            addStock(new Stock("MSFT", "Microsoft", 352.00));
            addStock(new Stock("TSLA", "Tesla", 265.75));
        }

        void addStock(Stock s) {
            stocks.put(s.symbol, s);
        }

        Stock getStock(String symbol) {
            return stocks.get(symbol.toUpperCase());
        }

        Collection<Stock> allStocks() {
            return stocks.values();
        }

        // simulate small random market movements for each tick
        void tick() {
            for (Stock s : stocks.values()) {
                // price movement -2% .. +2%
                double pct = (rnd.nextDouble() * 4.0) - 2.0;
                s.price = Math.max(0.01, s.price * (1.0 + pct / 100.0));
            }
        }
    }

    static class Portfolio {
        private final Map<String, Holding> holdings = new HashMap<>();
        private final List<Transaction> transactions = new ArrayList<>();
        double cash;

        Portfolio(double initialCash) {
            this.cash = initialCash;
        }

        Map<String, Holding> getHoldings() {
            return holdings;
        }

        List<Transaction> getTransactions() {
            return transactions;
        }

        boolean buy(String symbol, int shares, double pricePerShare) {
            double cost = shares * pricePerShare;
            if (shares <= 0 || cost > cash) return false;
            Holding h = holdings.get(symbol);
            if (h == null) {
                holdings.put(symbol, new Holding(symbol, shares, pricePerShare));
            } else {
                // update average price
                double totalCost = h.avgPrice * h.shares + cost;
                int totalShares = h.shares + shares;
                h.avgPrice = totalCost / totalShares;
                h.shares = totalShares;
            }
            cash -= cost;
            transactions.add(new Transaction("BUY", symbol, shares, pricePerShare));
            return true;
        }

        boolean sell(String symbol, int shares, double pricePerShare) {
            Holding h = holdings.get(symbol);
            if (h == null || shares <= 0 || shares > h.shares) return false;
            double proceeds = shares * pricePerShare;
            h.shares -= shares;
            if (h.shares == 0) holdings.remove(symbol);
            cash += proceeds;
            transactions.add(new Transaction("SELL", symbol, shares, pricePerShare));
            return true;
        }

        double portfolioMarketValue(Market market) {
            double total = cash;
            for (Holding h : holdings.values()) {
                Stock s = market.getStock(h.symbol);
                if (s != null) total += h.marketValue(s.price);
            }
            return total;
        }

        // save holdings to a simple CSV for persistence: symbol,shares,avgPrice,cash
        void saveToFile(String filename) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
                pw.println("CASH," + cash);
                for (Holding h : holdings.values()) {
                    pw.printf("%s,%d,%.4f%n", h.symbol, h.shares, h.avgPrice);
                }
            } catch (IOException e) {
                System.out.println("Failed to save portfolio: " + e.getMessage());
            }
        }

        // load holdings from file (if exists), update cash and holdings
        void loadFromFile(String filename) {
            File f = new File(filename);
            if (!f.exists()) return;
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                holdings.clear();
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length < 2) continue;
                    if ("CASH".equalsIgnoreCase(parts[0])) {
                        cash = Double.parseDouble(parts[1]);
                    } else if (parts.length >= 3) {
                        String sym = parts[0];
                        int sh = Integer.parseInt(parts[1]);
                        double avg = Double.parseDouble(parts[2]);
                        holdings.put(sym, new Holding(sym, sh, avg));
                    }
                }
            } catch (IOException e) {
                System.out.println("Failed to load portfolio: " + e.getMessage());
            }
        }
    }

    // --- Simple CLI application ---
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Market market = new Market();
        Portfolio portfolio = new Portfolio(10000.00); // start with 10k
        final String SAVE_FILE = "portfolio.txt";

        // load if saved before
        portfolio.loadFromFile(SAVE_FILE);

        System.out.println("Welcome to the Simple Stock Trading Platform!");
        boolean exit = false;
        while (!exit) {
            System.out.println("\n--- Main Menu ---");
            System.out.println("1) Display Market Data");
            System.out.println("2) Advance Market (simulate price change)");
            System.out.println("3) Buy Stock");
            System.out.println("4) Sell Stock");
            System.out.println("5) Portfolio Summary");
            System.out.println("6) Transaction History");
            System.out.println("7) Save Portfolio");
            System.out.println("8) Load Portfolio");
            System.out.println("0) Exit");
            System.out.print("Choose an option: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.println("\n-- Market Data --");
                    for (Stock s : market.allStocks()) {
                        System.out.println(s);
                    }
                    break;

                case "2":
                    market.tick();
                    System.out.println("Market advanced. Prices updated.");
                    break;

                case "3":
                    System.out.print("Enter stock symbol to BUY: ");
                    String bSym = sc.nextLine().toUpperCase();
                    Stock bStock = market.getStock(bSym);
                    if (bStock == null) {
                        System.out.println("Unknown symbol.");
                        break;
                    }
                    System.out.print("Enter number of shares to buy: ");
                    int bShares = readInt(sc);
                    if (bShares <= 0) {
                        System.out.println("Invalid share count.");
                        break;
                    }
                    double bPrice = bStock.price;
                    if (portfolio.buy(bSym, bShares, bPrice)) {
                        System.out.printf("Bought %d shares of %s @ %.2f%n", bShares, bSym, bPrice);
                    } else {
                        System.out.println("Buy failed (insufficient cash or invalid request).");
                    }
                    break;

                case "4":
                    System.out.print("Enter stock symbol to SELL: ");
                    String sSym = sc.nextLine().toUpperCase();
                    Stock sStock = market.getStock(sSym);
                    if (sStock == null) {
                        System.out.println("Unknown symbol.");
                        break;
                    }
                    System.out.print("Enter number of shares to sell: ");
                    int sShares = readInt(sc);
                    if (sShares <= 0) {
                        System.out.println("Invalid share count.");
                        break;
                    }
                    double sPrice = sStock.price;
                    if (portfolio.sell(sSym, sShares, sPrice)) {
                        System.out.printf("Sold %d shares of %s @ %.2f%n", sShares, sSym, sPrice);
                    } else {
                        System.out.println("Sell failed (not enough shares or invalid request).");
                    }
                    break;

                case "5":
                    System.out.println("\n-- Portfolio Summary --");
                    System.out.println("Cash: " + String.format("%.2f", portfolio.cash));
                    System.out.println("Holdings:");
                    if (portfolio.getHoldings().isEmpty()) {
                        System.out.println("  (no holdings)");
                    } else {
                        for (Holding h : portfolio.getHoldings().values()) {
                            Stock st = market.getStock(h.symbol);
                            double mPrice = (st != null) ? st.price : 0.0;
                            System.out.printf("  %s - %d shares, avg buy %.2f, market price %.2f, market value %.2f%n",
                                    h.symbol, h.shares, h.avgPrice, mPrice, h.marketValue(mPrice));
                        }
                    }
                    System.out.printf("Total Portfolio Value (cash + market holdings): %.2f%n",
                            portfolio.portfolioMarketValue(market));
                    break;

                case "6":
                    System.out.println("\n-- Transaction History --");
                    if (portfolio.getTransactions().isEmpty()) {
                        System.out.println("  (no transactions)");
                    } else {
                        for (Transaction t : portfolio.getTransactions()) {
                            System.out.println("  " + t);
                        }
                    }
                    break;

                case "7":
                    portfolio.saveToFile(SAVE_FILE);
                    System.out.println("Portfolio saved to " + SAVE_FILE);
                    break;

                case "8":
                    portfolio.loadFromFile(SAVE_FILE);
                    System.out.println("Portfolio loaded from " + SAVE_FILE);
                    break;

                case "0":
                    System.out.println("Saving portfolio and exiting...");
                    portfolio.saveToFile(SAVE_FILE);
                    exit = true;
                    break;

                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }

        sc.close();
    }

    private static int readInt(Scanner sc) {
        try {
            String s = sc.nextLine().trim();
            return Integer.parseInt(s);
        } catch (Exception e) {
            return -1;
        }
    }
}
