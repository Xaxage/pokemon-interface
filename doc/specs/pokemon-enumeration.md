# US-01 Pokémon Enumeration

Paginated, public browse of Pokémon. Each entry shows its sprite, category, weight and skills. Data comes
live from PokeAPI and is cached in memory.

> Terminology: PokeAPI defines **moves** as "the skills of Pokémon in battle", so *skills* means moves.

- **Rule: Must return Pokémon one page at a time, in PokeAPI order (by id), including alternate forms**

  | Page | Size | Entries returned | Has next page? |
    |---|---|---|---|
  | *(not given)* | *(not given)* | page 0 of 20: #1 Bulbasaur … #20 Raticate | yes |
  | 1 | 20 | #21 Spearow … #40 Wigglytuff | yes |
  | 0 | 100 | first 100 Pokémon (maximum size) | yes |
  | last page | 20 | only the remaining entries, ending with the alternate forms (ids 10001+) | no |
  | past the last page | 20 | none | no |

    - Counter-example: The one where an alternate form such as Mega Venusaur (id 10033) is listed as its own entry after
      all base Pokémon, not next to Venusaur.

- **Rule: Must tell the reader where they are in the full list**
    - Example: The one where page 0 of size 20 reports page 0, size 20, the total number of Pokémon as reported by
      PokeAPI, and the total number of pages.
    - Counter-example: The one where a page past the end comes back empty but still reports the real total and total
      pages.

- **Rule: Must show each Pokémon's default front sprite**
    - Example: The one where Bulbasaur comes back with the link to its default front battle sprite.
    - Counter-example: The one where an alternate form has no default front sprite. It is still listed, with an empty
      sprite.
  - Counter-example: The one where PokeAPI has no details for a listed Pokémon. It is still listed, with only its
    name and an empty sprite.

- **Rule: Must show each Pokémon's category as its English species genus**
    - Example: The one where Bulbasaur's category is "Seed Pokémon".
    - Example: The one where Alolan Raichu takes its species' category, "Mouse Pokémon", the same as Raichu.
    - Counter-example: The one where a species has no English genus. The Pokémon is still listed, with an empty
      category.

- **Rule: Must show each Pokémon's weight in hectograms, exactly as PokeAPI gives it**

  | Pokémon | Weight shown (hg) |
    |---|---|
  | Bulbasaur | 69 |
  | Gastly (lightest) | 1 |
  | Celesteela (heaviest) | 9999 |

- **Rule: Must show each Pokémon's skills (its moves) and how each is learned, in the newest version group known to
  PokeAPI**

  | Move learned by… | Shown as |
    |---|---|
  | levelling up | move name, "level-up", level learned (e.g. Tackle, level-up, 1) |
  | machine (TM) | move name, "machine", no level |
  | breeding | move name, "egg", no level |
  | tutor | move name, "tutor", no level |

    - Example: The one where a move can be learned two ways (by level-up and by machine), so it is shown once with both
      methods.
    - Counter-example: The one where a Pokémon does not appear in the newest version group. It is still listed, with no
      skills.
    - Counter-example: The one where a move the Pokémon could learn only in older games is left out.

- **Rule: Must reject pagination requests that are not valid (400, with the standard error body)**

  | Page | Size | Outcome |
    |---|---|---|
  | -1 | 20 | rejected |
  | 0 | 0 | rejected |
  | 0 | 101 | rejected |
  | abc | 20 | rejected |
  | 0 | 100 | accepted (maximum size) |

- **Rule: Must let anyone browse, signed in or not**
    - Example: The one where a visitor with no token gets page 0.

- **Rule: Should reuse PokeAPI responses for 24 hours instead of asking again**
    - Example: The one where the same page is browsed twice within 24 hours, and PokeAPI is called only the first time.
    - Counter-example: The one where more than 24 hours have passed, so PokeAPI is called again and the fresh data is
      shown.

- **Rule: Must keep serving cached data while PokeAPI is unavailable, and otherwise fail clearly**
    - Example: The one where PokeAPI is down but the requested page is still cached, so the page is served as normal.
    - Counter-example: The one where PokeAPI is down and the page is not cached, so the reader gets a 503 with the
      standard error body and no upstream details.