# Kata 9 Coding Practice

see [Kata09: Back to the Checkout](http://codekata.com/kata/kata09-back-to-the-checkout/) for the problem description.

## How to run
Since no requirements were given I decided to use the latest Java LTS (Java 17). This conveniently allowed me to use records for data objects.
An exising maven installation is required.

Cmd: ```mvn test``` 


## Solution Approach
I decided to NOT provide an artificially bloated Spring Boot based REST service because it actually won't show much in terms of problem-solving and would be rather meaningless without actually business requirements.
Instead, I decided to extend the scope differently, by supporting not pricing rules based on counting (QUANTITY) but also based on other units (e.g. WEIGHT).

The approach does not require any knowledge about the used units, except if they are dividable (e.g. a weight if 1 kg can be divided into arbitrary amount, e.g. 123g of ham) or not (e.g. counting apples) and can be easily extended.



### Algorithm
I always prefer the simplest solution for a given problem, as long as it is easily readable and extendable if needed.
I decided against a recursive algorithm which some might consider more elegant but would be harder to follow for somebody new to the problem (e.g. a new team member).

The Checkout used a nested Map as data holder and sums up an article's amount when a new article is added to the checkout.
To properly support different units a Measurement Result needs to be provided in a scan action. For the simple "count" use case (unit=QUANTITY, amount=1) convenience methods are also available.

During price calculation the algorithm calculates subtotals for each SKU/Unit pair and then sum them it to subTotals per SKU and eventually a total.
It simply applies all matching pricing rules based on the rule's amount (starting with the largest) and subtract it from the SKU amount, until no remaining SKU amount is left.

For dividable Units like WEIGHT the prince of any remaining amount is calculated based on the last rule (the rule with the smallest amount).

### Data structure/types and validation
Although using a ```BigDecimal``` for monetary amount has its own problems I believe it's better than the alternative (float/double due to its imprecision, ints/long with cents values due to its lack of clarity).
For the original scope of this exercise is not even required (the precision of a float/double would be sufficient) I use it anyway, mainly to make a point of NEVER use a float for money. PERIOD.


All input parameters are validated. I do remember you've said "production code" with the example of "No NullPointerException."
In my opinion a NullPointerException in itself is not bad at all, only if it happens accidentally... ;-)
I make regular use of them for simple param validation (by using Objects.requireNonNull()) so it will them encounter in my code but on purpose!


## Tests
In addition to [JUnit5](https://junit.org/junit5/) I also use [Hamcrest](http://hamcrest.org/JavaHamcrest/) for easier validation of BigDecimals objects (the known problem with its discussable equals approach...).
Since the total price is actually rounded to two decimal places the scale is now fix and Hamcrest could be replaced with a simple assertEquals.
However, in many real-world applications the scale is considered unimportant (as long as it is not displayed to a user) as long as the value is correct, so I left it in.

For the reader's convenience the test structure follows closely the test outlined in the practise.

