# Future Test Ideas and Improvements

Based on the extensive JSON parser research in CLAUDE.md, here are additional tests that could be implemented to enhance the extension's coverage.

## Additional Test Categories

### 9. Encoding Variations
**Description**: Test different character encodings and BOM handling

Tests to implement:
- UTF-8 with BOM at start
- UTF-16 LE/BE encoding
- UTF-32 encoding
- Mixed encoding within same JSON
- Invalid UTF-8 sequences
- Over-long UTF-8 sequences
- Modified UTF-8 (CESU-8)

**Example**:
```json
// With UTF-8 BOM
\xEF\xBB\xBF{"username":"test"}
```

### 10. String Escape Sequence Variations
**Description**: Test various escape sequence handling

Tests to implement:
- `\x` hex escapes (non-standard)
- `\0` null character
- `\v` vertical tab
- `\f` form feed
- `\b` backspace
- Mixed escaped/unescaped quotes
- Escaped forward slash `\/` vs `/`
- Invalid escape sequences

**Example**:
```json
{"test": "value\x00with\x0anull"}
```

### 11. Case Sensitivity Tests
**Description**: Test key case sensitivity differences

Tests to implement:
- Duplicate keys with different cases
- Mixed case in boolean/null literals
- Case variations in scientific notation

**Example**:
```json
{"User": "admin", "user": "attacker"}
{"value": True}  // vs true
{"number": 1E5}  // vs 1e5
```

### 12. Special Number Formats
**Description**: Test non-standard number representations

Tests to implement:
- Hexadecimal numbers: `0x1A`
- Octal numbers: `0o17`
- Binary numbers: `0b1010`
- Leading zeros: `0123`
- Leading plus: `+123`
- Multiple signs: `--5`
- Infinity: `Infinity`, `-Infinity`
- NaN: `NaN`

**Example**:
```json
{"price": 0x100}
{"quantity": +5}
{"value": NaN}
```

### 13. Trailing Commas
**Description**: Test trailing comma handling

Tests to implement:
- Trailing comma in objects
- Trailing comma in arrays
- Multiple trailing commas
- Leading commas

**Example**:
```json
{"key": "value",}
[1, 2, 3,]
```

### 14. Key-Value Separator Variations
**Description**: Test colon variations and alternatives

Tests to implement:
- Multiple colons: `"key"::value`
- Equals sign: `"key"=value`
- Missing colon: `"key"value`
- Whitespace in colon: `"key" : value`

**Example**:
```json
{"key"=value}
{"key" value}
```

## Enhanced Existing Categories

### String Variations Enhancements
- [ ] All-uppercase keys vs lowercase
- [ ] Snake_case vs camelCase transformations
- [ ] Emoji in keys/values
- [ ] RTL (right-to-left) characters
- [ ] Zero-width characters
- [ ] Homograph attacks (similar-looking characters)

**Example**:
```json
{"user\u200Bname": "admin"}  // Zero-width space
{"раge": 1}  // Cyrillic 'а' instead of Latin 'a'
```

### Number Variations Enhancements
- [ ] Negative zero: `-0`
- [ ] Positive zero: `+0`
- [ ] Very small numbers: `1e-9999`
- [ ] Fractional boundaries: `0.9999999999999999`
- [ ] Integer/float confusion: `1` vs `1.0`

**Example**:
```json
{"value": -0}
{"price": 1.0000000000000001}
```

### Unicode Enhancements
- [ ] Normalization form variations (NFC, NFD, NFKC, NFKD)
- [ ] Combining characters
- [ ] Ligatures
- [ ] Grapheme clusters
- [ ] Bidirectional text markers

**Example**:
```json
{"café": "value"}  // é as single char vs e + combining accent
```

## CVE-Specific Test Patterns

### Pattern 1: Erlang-Style Role Bypass
Based on CVE example in documentation:

```json
{
  "type": "user",
  "name": "attacker",
  "roles": ["admin"],
  "roles": []
}
```

**Generator Logic**:
- Find array fields
- Create duplicate with empty array
- Create duplicate with privileged values

### Pattern 2: E-commerce Quantity Bypass
Based on payment processing example:

```json
{
  "cart": [
    {
      "id": 1,
      "qty": -1,
      "qty": 1
    }
  ]
}
```

**Generator Logic**:
- Find numeric fields
- Create negative duplicate
- Create positive duplicate
- Test validation bypass

### Pattern 3: Character Truncation Exploit
Based on Unicode truncation example:

```json
{"username": "admin\ud800"}
```

**Generator Logic**:
- Append truncation characters to sensitive fields
- Test: `\ud800`, `\x0d`, `\"`, `\\`

## Parser-Specific Tests

### Tests for Different Parser Behaviors

#### First-Key Precedence Parsers
```json
{"role": "admin", "role": "user"}  // admin wins
```

#### Last-Key Precedence Parsers
```json
{"role": "user", "role": "admin"}  // admin wins
```

#### Concatenation Parsers
```json
{"value": "first", "value": "second"}
// Some parsers: "firstsecond"
```

#### Array Creation Parsers
```json
{"value": "first", "value": "second"}
// Some parsers: ["first", "second"]
```

## Validation Bypass Patterns

### Input Validation Bypass
```json
{
  "email": "valid@test.com",
  "email\u0000": "admin@internal.com"
}
```

### Type Confusion
```json
{
  "admin": false,
  "admin": "false"  // String "false" might evaluate to true
}
```

### SQL Injection via Numbers
```json
{
  "id": 1,
  "id": "1 OR 1=1--"
}
```

## Advanced Fuzzing Strategies

### 1. Polyglot Payloads
Combine multiple exploit types:
```json
{
  "user\u0000admin": "value",
  "user\u0000admin": "attacker"
}
```

### 2. Nested Duplicate Keys
```json
{
  "user": {
    "role": "user",
    "role": "admin"
  }
}
```

### 3. Mixed Type Duplicates
```json
{
  "value": 123,
  "value": "123",
  "value": true,
  "value": null
}
```

### 4. Complexity Bombs
```json
{
  "a": "value",
  "a": "value",
  "a": "value",
  ... // 1000+ duplicates
}
```