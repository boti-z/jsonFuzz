# jsonScan - JSON Fuzzer Usage Guide

## Overview

jsonScan is a comprehensive JSON fuzzer for Burp Suite that generates hundreds of JSON permutations to test how backend parsers handle edge cases, malformed data, and ambiguous JSON structures.

## Quick Start

1. **Build and Load**:
   ```bash
   ./gradlew jar
   ```
   Then load `build/libs/extension-template-project.jar` in Burp Suite

2. **Configure**: Go to "jsonScan Settings" tab and select test categories

3. **Fuzz**: Right-click any JSON request → "jsonScan: Fuzz!"

4. **Analyze**: Review results in "jsonScan Results" tab

## Test Categories

### 1. String Variations (7 tests per string)
- Unicode encoding of last character
- Unpaired surrogate (`\ud888`)
- Null byte injection (`\u0000ef`)
- Wrap in array
- Wrap in object
- Array with extra value
- Partial Unicode conversion

### 2. Number Edge Cases (9 tests per number)
- High precision float: `3.141592653589793238462643383279`
- Very large exponent: `1.0e4096`
- Scientific notation: `6e23`
- Negative exponent: `1.602e-19`
- Decimal with exponent: `123.456e7`
- Integer boundary: `9007199254740990`
- Beyond boundary: `9007199254740993`
- Negative boundaries

### 3. Duplicate Keys (5 tests per key)
- Simple duplicate with different value
- Duplicate with same value
- Unicode variation of key
- Unpaired surrogate in key
- Null byte in key

### 4. Unicode Attacks (7 tests per string)
- Unpaired surrogate `\ud800`
- Carriage return in key
- Stray backslash
- Stray quote in key
- BOM at start `\uFEFF`
- Incomplete Unicode escape
- Backslash as Unicode `\u005C`

### 5. Comment Injection (7 tests)
- Single-line comment at start
- Multi-line comment at start
- Comment between properties
- Comment after colon
- Comment-like text in value
- Nested comments

### 6. Whitespace Variations (7 tests)
- Replace spaces with tabs
- Newlines after structural characters
- Excessive whitespace
- Mixed whitespace
- Windows-style CRLF
- Remove all whitespace (minified)
- Unicode whitespace characters

### 7. Deep Nesting (12 tests)
- Deep objects: 50, 100, 500, 1000 levels
- Deep arrays: 50, 100, 500, 1000 levels
- Mixed nesting: 50, 100, 500, 1000 levels

### 8. Array/Object Duplication
- Duplicate array elements (2x, 3x)
- Duplicate objects with suffix keys
- Nested structure duplication

## Understanding Results

### Results Table Columns

- **ID**: Unique permutation identifier
- **Category**: Test category that generated this
- **Description**: Specific test applied
- **Status**: HTTP status code or ERROR
- **Length**: Response body length
- **Time (ms)**: Response time
- **Anomalies**: Number of detected anomalies

### Anomaly Types

- **Status Code Change**: Different from baseline
- **Error Response**: Contains error keywords
- **Length Variance**: >10% size difference
- **Timeout**: Request timed out
- **Connection Error**: Network failure
- **Content-Type Change**: Different Content-Type header

### Interpreting Anomalies

🔴 **Red highlighting** = Anomaly detected

**High Priority**:
- Status code changes (200 → 400, 200 → 500)
- Error responses that baseline didn't have
- Significant length differences

**Medium Priority**:
- Small length variances
- Timeout on specific permutations

**Low Priority**:
- No anomaly (baseline behavior maintained)

## Real-World Examples

### Example 1: Duplicate Key Exploit

**Original JSON**:
```json
{"role": "user", "permissions": ["read"]}
```

**Permutation** (Duplicate Keys):
```json
{"role": "user", "permissions": ["read"], "role": "admin"}
```

**Result**: If backend uses last-key precedence but validation uses first-key, attacker gains admin access.

### Example 2: Unicode Truncation

**Original JSON**:
```json
{"username": "testuser"}
```

**Permutation** (Unicode Attack):
```json
{"username": "admin\ud888"}
```

**Result**: If parser truncates at unpaired surrogate, creates username "admin" bypassing uniqueness checks.

### Example 3: Number Boundary

**Original JSON**:
```json
{"quantity": 5}
```

**Permutation** (Number Edge):
```json
{"quantity": 9007199254740993}
```

**Result**: Parser A sees correct value, Parser B rounds differently, causing inventory discrepancies.

## Performance Tips

### Estimating Permutations

Use "jsonScan: Estimate Permutations" to preview count:

- Simple JSON (2-3 keys): ~100-200 permutations
- Medium JSON (5-10 keys): ~300-500 permutations
- Complex JSON (15+ keys): ~800-1500 permutations

### Optimization Strategies

1. **Disable unnecessary categories**: If testing specific vulnerability, disable other categories
2. **Start small**: Test with simple JSON first
3. **Use filters**: Enable "Show only anomalies" to focus on interesting results
4. **Background processing**: Extension runs in background, Burp remains responsive

### Recommended Workflows

**Quick Scan** (fast):
- Enable: Duplicate Keys, Unicode Attacks
- ~50-100 permutations

**Thorough Scan** (comprehensive):
- Enable: All categories
- ~500-1500 permutations

**Custom Scan** (targeted):
- Enable specific categories based on target
- E.g., Number Edge Cases for pricing APIs

## Common Issues

### No Context Menu

**Problem**: Right-click doesn't show "jsonScan: Fuzz!"

**Solutions**:
- Verify request has `Content-Type: application/json`
- Check request body is valid JSON
- Reload extension in Burp

### Too Many Permutations

**Problem**: Estimate shows 2000+ permutations

**Solutions**:
- Disable some test categories
- Consider if all tests are necessary
- Be patient - extension handles large sets

### No Anomalies Found

**Problem**: All tests return same response as baseline

**Solutions**:
- This is normal! It means backend parser is robust
- Try different endpoints
- Check if responses are actually identical (length, status)

## Advanced Usage

### Filtering Results

1. Click column headers to sort
2. Use "Show only anomalies" checkbox
3. Click rows to inspect full request/response

### Exporting Results

Currently manual:
1. Select interesting results
2. Copy request/response from editors
3. Document findings externally

### Integration with Other Tools

jsonScan works alongside:
- **Repeater**: Send interesting permutations to Repeater
- **Intruder**: Use findings to build targeted attacks
- **Logger**: All requests logged automatically
- **Scanner**: Complement Scanner findings

## Best Practices

1. **Always test baseline first**: Verify original request works
2. **Document anomalies**: Note which categories trigger issues
3. **Verify findings**: Manually test interesting anomalies
4. **Test responsibly**: Only test authorized targets
5. **Share findings**: Report to security teams/bug bounty programs

## Troubleshooting

### Extension Won't Load

- Check Java version (needs Java 21+)
- Review Burp extender output tab for errors
- Verify JAR was built correctly

### Build Errors

```bash
./gradlew clean build --info
```

Check for dependency or compilation errors

### Slow Performance

- Reduce enabled test categories
- Check network latency to target
- Monitor Burp memory usage

## Support

- Check Burp extender output for error messages
- Review CLAUDE.md for detailed documentation
- Examine esqueleto.py for original Python implementation
