package org.example.banking.utils;

import java.security.SecureRandom;

public final class AccountNumberGenerator {

    private static final int ACCOUNT_NUMBER_LENGTH = 15;
    private static final SecureRandom RANDOM = new SecureRandom();

    private AccountNumberGenerator() {
    }

    public static String generateAccountNumber() {
        StringBuilder accountNumber = new StringBuilder();

        // Generate the first 14 digits.
        accountNumber.append(RANDOM.nextInt(9) + 1);

        for (int i = 1; i < ACCOUNT_NUMBER_LENGTH - 1; i++) {
            accountNumber.append(RANDOM.nextInt(10));
        }

        // Append the Luhn check digit.
        int checkDigit = calculateCheckDigit(accountNumber.toString());
        accountNumber.append(checkDigit);

        return accountNumber.toString();
    }

    private static int calculateCheckDigit(String digits) {
        int sum = 0;
        boolean doubleDigit = true;

        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';

            if (doubleDigit) {
                digit *= 2;

                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            doubleDigit = !doubleDigit;
        }

        return (10 - (sum % 10)) % 10;
    }
}
