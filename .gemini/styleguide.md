# Gemini Code Assist Style Guide

## Language

Please provide all code reviews, summaries, and help messages in Korean (한글).

# Introduction
This style guide outlines the coding conventions for Python code developed at Company X.
It's based on PEP 8, but with some modifications to address specific needs and
preferences within our organization.

# Key Principles
* **Readability:** Code should be easy to understand for all team members.
* **Maintainability:** Code should be easy to modify and extend.
* **Consistency:** Adhering to a consistent style across all projects improves
  collaboration and reduces errors.
* **Performance:** While readability is paramount, code should be efficient.

# Deviations from PEP 8

## Line Length
* **Maximum line length:** 100 characters (instead of PEP 8's 79).
    * Modern screens allow for wider lines, improving code readability in many cases.
    * Many common patterns in our codebase, like long strings or URLs, often exceed 79 characters.

## Indentation
* **Use 4 spaces per indentation level.** (PEP 8 recommendation)

## Imports
* **Group imports:**
    * Standard library imports
    * Related third party imports
    * Local application/library specific imports
* **Absolute imports:** Always use absolute imports for clarity.
* **Import order within groups:**  Sort alphabetically.

## Naming Conventions

* **Variables:** Use lowercase with underscores (snake_case): `user_name`, `total_count`
* **Constants:**  Use uppercase with underscores: `MAX_VALUE`, `DATABASE_NAME`
* **Functions:** Use lowercase with underscores (snake_case): `calculate_total()`, `process_data()`
* **Classes:** Use CapWords (CamelCase): `UserManager`, `PaymentProcessor`
* **Modules:** Use lowercase with underscores (snake_case): `user_utils`, `payment_gateway`

## Docstrings
* **Use triple double quotes (`"""Docstring goes here."""`) for all docstrings.**
* **First line:** Concise summary of the object's purpose.
* **For complex functions/classes:** Include detailed descriptions of parameters, return values,
  attributes, and exceptions.
* **Use Google style docstrings:** This helps with automated documentation generation.
    ```python
    def my_function(param1, param2):
        """Single-line summary.

        More detailed description, if necessary.

        Args:
            param1 (int): The first parameter.
            param2 (str): The second parameter.

        Returns:
            bool: The return value. True for success, False otherwise.

        Raises:
            ValueError: If `param2` is invalid.
        """
        # function body here
    ```

## Type Hints
* **Use type hints:**  Type hints improve code readability and help catch errors early.
* **Follow PEP 484:**  Use the standard type hinting syntax.

## Comments
* **Write clear and concise comments:** Explain the "why" behind the code, not just the "what".
* **Comment sparingly:** Well-written code should be self-documenting where possible.
* **Use complete sentences:** Start comments with a capital letter and use proper punctuation.

## Logging
* **Use a standard logging framework:**  Company X uses the built-in `logging` module.
* **Log at appropriate levels:** DEBUG, INFO, WARNING, ERROR, CRITICAL
* **Provide context:** Include relevant information in log messages to aid debugging.

## Error Handling
* **Use specific exceptions:** Avoid using broad exceptions like `Exception`.
* **Handle exceptions gracefully:** Provide informative error messages and avoid crashing the program.
* **Use `try...except` blocks:**  Isolate code that might raise exceptions.

# Tooling
* **Code formatter:**  [Specify formatter, e.g., Black] - Enforces consistent formatting automatically.
* **Linter:**  [Specify linter, e.g., Flake8, Pylint] - Identifies potential issues and style violations.