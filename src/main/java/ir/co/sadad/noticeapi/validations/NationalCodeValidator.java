package ir.co.sadad.noticeapi.validations;

import org.springframework.stereotype.Component;

@Component
public class NationalCodeValidator {

    public static boolean isValid(String nationalCode) {
        if (nationalCode == null || nationalCode.equals("")) {
            return false;
        }

        if (nationalCode.length() != 10) {
            return false;
        }
        if (!nationalCode.matches("^\\d{10}$")) {
            return false;
        }

        int sum = 0;
        int lenght = 10;
        for (int i = 0; i < lenght - 1; i++) {
            sum += Integer.parseInt(String.valueOf(nationalCode.charAt(i))) * (lenght - i);
        }

        int r = Integer.parseInt(String.valueOf(nationalCode.charAt(9)));

        int c = sum % 11;

        return (((c < 2) && (r == c)) || ((c >= 2) && ((11 - c) == r)));
    }
}
