package com.example.marriagegender;

import java.util.Locale;

public enum Gender {

    NONE,
    MALE,
    FEMALE;

    public static Gender fromInput(String input) {
        if (input == null) {
            return null;
        }

        String value = input.trim().toLowerCase(Locale.ROOT);

        switch (value) {
            case "male":
            case "m":
            case "man":
            case "boy":
            case "мужской":
            case "мужчина":
            case "м":
                return MALE;

            case "female":
            case "f":
            case "woman":
            case "girl":
            case "женский":
            case "женщина":
            case "ж":
                return FEMALE;

            default:
                return null;
        }
    }

    public String displayName() {
        switch (this) {
            case MALE:
                return "Мужской";
            case FEMALE:
                return "Женский";
            default:
                return "Не выбран";
        }
    }
}
