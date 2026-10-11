package com.example.parser

data class SampleSms(
    val title: String,
    val bankName: String,
    val message: String
)

object SampleSmsData {
    val samples = listOf(
        SampleSms(
            title = "Groceries (Trader Joe's)",
            bankName = "Chase Bank",
            message = "Chase Alert: You spent $86.45 at Trader Joe's on 08-Oct-2026 with debit card ending 4821. Avail balance: $2,840.10. Ref: CH982314."
        ),
        SampleSms(
            title = "Dining (Starbucks)",
            bankName = "Wells Fargo",
            message = "Wells Fargo: $6.75 was charged at Starbucks on 10/09/2026 on your Visa card 7712. Text STOP to cancel alerts."
        ),
        SampleSms(
            title = "Shopping (Amazon)",
            bankName = "Bank of America",
            message = "BofA Debit Alert: $149.99 paid to Amazon.com on 07-Oct-2026 from Checking A/c *3490. Txn ID: AMZ782910."
        ),
        SampleSms(
            title = "Transport (Uber Ride)",
            bankName = "Capital One",
            message = "Capital One: Purchase of $32.40 approved at Uber on 10/10/2026 with Card ending in 6604."
        ),
        SampleSms(
            title = "Utilities (Electric Bill)",
            bankName = "Citi Card",
            message = "Citi Alert: Payment of $115.50 to Con Edison Electric was debited on 05-Oct-2026. Ref: CON991823."
        ),
        SampleSms(
            title = "Entertainment (Netflix)",
            bankName = "Apple Card",
            message = "Apple Card: You spent $15.49 at Netflix on 03-Oct-2026. 2% Daily Cash earned."
        ),
        SampleSms(
            title = "Healthcare (CVS Pharmacy)",
            bankName = "Chase Bank",
            message = "Chase Freedom: $48.20 charged at CVS Pharmacy on 06-Oct-2026. Card ending 4821."
        ),
        SampleSms(
            title = "Fuel (Shell Gas Station)",
            bankName = "Discover",
            message = "Discover Card: $52.00 paid at Shell Oil on 04-Oct-2026. Available credit: $5,240.00."
        ),
        SampleSms(
            title = "Salary Deposit",
            bankName = "Direct Deposit",
            message = "Direct Deposit Alert: USD 3,850.00 credited to Checking A/c *3490 from ACME Corp Payroll on 01-Oct-2026. Ref: DIR882103."
        ),
        SampleSms(
            title = "Non-Transaction OTP (Ignored)",
            bankName = "Security Alert",
            message = "Your verification code is 639102. Valid for 5 minutes. Do not share this OTP with anyone including bank staff."
        )
    )
}
