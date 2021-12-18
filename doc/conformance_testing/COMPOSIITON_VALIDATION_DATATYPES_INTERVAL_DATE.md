## 3.10. quantity.DV_INTERVAL<DV_DATE>

TBD: this will use the test cases and data sets defined for the DV_DATE tests.


### 3.10.1. Test case DV_INTERVAL<DV_DATE> open constraint

On this case, the own rules/invariants of the DV_INTERVAL apply to the validation.

| lower      | upper      | lower_unbounded | upper_unbounded | lower_included | upper_included | expected | constraints violated          |
|:----------:|:----------:|-----------------|-----------------|----------------|----------------|----------|-------------------------------|
| NULL       | NULL       | false           | false           | true           | true           | rejected | IMO should fail, see https://discourse.openehr.org/t/is-dv-interval-missing-invariants/2210 |
| NULL       | 2022       | false           | false           | true           | true           | rejected | IMO should fail, see https://discourse.openehr.org/t/is-dv-interval-missing-invariants/2210 |
| 2021       | NULL       | false           | false           | true           | true           | rejected | IMO should fail, see https://discourse.openehr.org/t/is-dv-interval-missing-invariants/2210 |
| 2021       | 2022       | false           | false           | true           | true           | accepted |  |
| 2021-01    | 2022-08    | false           | false           | true           | true           | accepted |  |
| 2021-01-20 | 2022-08-11 | false           | false           | true           | true           | accepted |  |

| 2021       | 2022-10    | false           | false           | true           | true           | rejected | IMO two dates with different components shouldn't be strictly comparable, see https://discourse.openehr.org/t/issues-with-date-time-comparison-for-partial-date-time-expressions/2173 |
| NULL       | NULL       | true            | true            | false          | false          | accepted |  |


### 3.10.2. Test case DV_INTERVAL<DV_DATE> validity kind constraint

```
NOTE: this test case doesn't include all the possible combinations of lower/upper data and constraints for the internal since there could be tens of possible combinations. It would be in the scope of a revision to add more combinations of an exhaustive test case.
```

| lower      | upper      | lower_unbounded | upper_unbounded | lower_included | upper_included | month_val. (lower) | day_val. (lower) | month_val. (upper) | day_val. (upper) | expected | constraints violated          |
|:----------:|:----------:|-----------------|-----------------|----------------|----------------|--------------------|------------------|--------------------|------------------|----------|-------------------------------|
| 2021       | 2022       | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | mandatory        | rejected | month_validity (lower), day_validity (lower), month_validity (upper), day_validity (upper) |
| 2021       | 2022       | false           | false           | true           |true            | mandatory          | optional         | mandatory          | optional         | rejected | month_validity (lower), month_validity (upper) |
| 2021       | 2022       | false           | false           | true           |true            | optional           | optional         | optional           | optional         | accepted |                               |
| 2021       | 2022       | false           | false           | true           |true            | mandatory          | prohibited       | mandatory          | prohibited       | rejected | month_validity (lower), month_validity (upper) |
| 2021       | 2022       | false           | false           | true           |true            | prohibited         | prohibited       | prohibited         | prohibited       | accepted |                               |
| 2021-10    | 2022-10    | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | mandatory        | rejected | day_validity (lower), day_validity (upper)     |
| 2021-10    | 2022-10    | false           | false           | true           |true            | mandatory          | optional         | mandatory          | optional         | accepted |                               |
| 2021-10    | 2022-10    | false           | false           | true           |true            | optional           | optional         | optional           | optional         | accepted |                               |
| 2021-10    | 2022-10    | false           | false           | true           |true            | mandatory          | prohibited       | mandatory          | prohibited       | accepted |                               |
| 2021-10    | 2022-10    | false           | false           | true           |true            | prohibited         | prohibited       | prohibited         | prohibited       | rejected | month_validity (lower), month_validity (upper) |
| 2021-10-24 | 2022-10-24 | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | mandatory        | accepted |                               |
| 2021-10-24 | 2022-10-24 | false           | false           | true           |true            | mandatory          | optional         | mandatory          | optional         | accepted |                               |
| 2021-10-24 | 2022-10-24 | false           | false           | true           |true            | optional           | optional         | optional           | optional         | accepted |                               |
| 2021-10-24 | 2022-10-24 | false           | false           | true           |true            | mandatory          | prohibited       | mandatory          | prohibited       | rejected | day_validity (lower), day_validity (upper)     |
| 2021-10-24 | 2022-10-24 | false           | false           | true           |true            | prohibited         | prohibited       | prohibited         | prohibited       | rejected | month_validity (lower), day_validity (lower), month_validity (upper), day_validity (upper) |
| 2021       | 2022       | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | optional         | rejected | month_validity (lower), day_validity (lower), month_validity (upper) |
| 2021       | 2022       | false           | false           | true           |true            | mandatory          | mandatory        | optional           | optional         | rejected | month_validity (lower), day_validity (lower)                         |
| 2021       | 2022       | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | prohibited       | rejected | month_validity (lower), day_validity (lower), month_validity (upper) |
| 2021       | 2022       | false           | false           | true           |true            | mandatory          | mandatory        | prohibited         | prohibited       | rejected | month_validity (lower), day_validity (lower)                         |
| 2021       | 2022-10    | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | mandatory        | rejected | month_validity (lower), day_validity (lower), day_validity (upper)   |
| 2021       | 2022-10    | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | optional         | rejected | month_validity (lower), day_validity (lower)                         |
| 2021       | 2022-10    | false           | false           | true           |true            | mandatory          | mandatory        | optional           | optional         | rejected | month_validity (lower), day_validity (lower)                         |
| 2021       | 2022-10    | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | prohibited       | rejected | month_validity (lower), day_validity (lower)                         |
| 2021       | 2022-10    | false           | false           | true           |true            | mandatory          | mandatory        | prohibited         | prohibited       | rejected | month_validity (lower), day_validity (lower), month_validity (upper) |
| 2021       | 2022-10-24 | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | mandatory        | rejected | month_validity (lower), day_validity (lower)                         |
| 2021       | 2022-10-24 | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | optional         | rejected | month_validity (lower), day_validity (lower)                         |
| 2021       | 2022-10-24 | false           | false           | true           |true            | mandatory          | mandatory        | optional           | optional         | rejected | month_validity (lower), day_validity (lower)                         |
| 2021       | 2022-10-24 | false           | false           | true           |true            | mandatory          | mandatory        | mandatory          | prohibited       | rejected | month_validity (lower), day_validity (lower), day_validity (upper)   |
| 2021       | 2022-10-24 | false           | false           | true           |true            | mandatory          | mandatory        | prohibited         | prohibited       | rejected | month_validity (lower), day_validity (lower), month_validity (upper), day_validity (upper) |



### 3.10.3. Test case DV_INTERVAL<DV_DATE> validity range constraint

| lower      | upper      | lower_unbounded | upper_unbounded | lower_included | upper_included | C_DATE.range (lower) | C_DATE.range (upper) | expected | constraints violated          |
|:----------:|:----------:|-----------------|-----------------|----------------|----------------|----------------------|----------------------|----------|---------------------------|
| 2021       | 2022       | false           | false           | true           | true           | 1900..2030           | 1900..2030           | accepted |         |
| 2021       | 2022       | false           | false           | true           | true           | 2022..2030           | 1900..2030           | rejected | C_DATE.range (lower)        |
| 2021       | 2022       | false           | false           | true           | true           | 1900..2030           | 2023..2030           | rejected | C_DATE.range (upper)         |
| 2021       | 2022       | false           | false           | true           | true           | 2022..2030           | 2023..2030           | rejected | C_DATE.range (lower), C_DATE.range (upper)         |

