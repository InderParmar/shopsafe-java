package com.shopsafe.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * TestDataReader — loads JSON test-data files into typed Java lists.
 *
 * Python equivalent: utils/data_reader.py
 *
 * In Python, json.load() returns dicts. Here, Jackson's ObjectMapper
 * deserialises JSON arrays into typed Java POJOs (LoginData, CheckoutData, etc.),
 * which gives compile-time field access instead of dict["key"] string lookups.
 *
 * Usage:
 *   List<LoginData> cases = TestDataReader.read("login_data.json", LoginData.class);
 *   for (LoginData c : cases) { loginPage.login(c.username, c.password); }
 */
public class TestDataReader {

    // Shared ObjectMapper — thread-safe and expensive to create, so we reuse one instance.
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Base path for all test-data files, relative to the project root.
    // Matches the test-data/ directory in the repo.
    private static final String DATA_DIR = "test-data/";

    private TestDataReader() {}

    /**
     * Read a JSON array from a file and return it as a typed List.
     *
     * @param filename  the JSON filename, e.g. "login_data.json"
     * @param itemClass the POJO class representing one item in the array
     * @param <T>       the item type, inferred from itemClass
     * @return a List of deserialized POJOs
     */
    public static <T> List<T> read(String filename, Class<T> itemClass) {
        try {
            // constructCollectionType tells Jackson to deserialize a JSON array
            // into List<T> where T is itemClass — e.g. List<LoginData>.
            return MAPPER.readValue(
                new File(DATA_DIR + filename),
                MAPPER.getTypeFactory().constructCollectionType(List.class, itemClass)
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to read test data file: " + filename, e);
        }
    }

    // ── Inner POJO classes ────────────────────────────────────────────────────
    // Each class maps to one JSON object in the test-data arrays.
    // Jackson uses the public fields to bind JSON keys → Java fields.
    // No getters/setters required when fields are public.

    /**
     * Maps to one entry in login_data.json:
     *   { "username": "standard_user", "password": "secret_sauce" }
     *
     * Python equivalent: the dict produced by json.load() in data_reader.py
     */
    public static class LoginData {
        public String username;
        public String password;
        // No-arg constructor required by Jackson for deserialization.
        public LoginData() {}
    }

    /**
     * Maps to one entry in checkout_data.json:
     *   { "first_name": "John", "last_name": "Doe", "postal_code": "M5V2T6", "expected": "success" }
     *
     * The "expected" field drives parametrized test branching:
     *  - "success"   → assert order confirmation page
     *  - "zip_error" → assert postal-code validation error message
     *
     * Python equivalent: the e2e_data.json entries read by data_reader.py
     */
    public static class CheckoutData {
        public String firstName;   // mapped from JSON key "first_name" — see @JsonProperty below
        public String lastName;
        public String postalCode;
        public String expected;

        public CheckoutData() {}
    }
}
