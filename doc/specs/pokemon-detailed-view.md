# US-02 Detailed View

Public view of one Pokémon: its image, core stats, description and evolution chain. Data comes live from
PokeAPI and is cached in memory, the same as US-01.

- **Rule: Must show a chosen Pokémon by its id**
    - Example: The one where id 1 returns Bulbasaur.
    - Counter-example: The one where no Pokémon has the id (e.g. 99999), so the reader gets a 404 with the standard
      error body.

- **Rule: Must show the Pokémon's official artwork as its image**
    - Example: The one where Bulbasaur comes back with the link to its official artwork.
    - Counter-example: The one where a Pokémon has no official artwork, so its default front sprite is shown instead.

- **Rule: Must show the six core stats with their base values**

  | Stat | Bulbasaur |
    |---|---|
  | hp | 45 |
  | attack | 49 |
  | defense | 49 |
  | special-attack | 65 |
  | special-defense | 65 |
  | speed | 45 |

- **Rule: Must show an English description from the Pokémon's species, taken from the newest game that has one**
    - Example: The one where Bulbasaur's description is a single line of plain text, with PokeAPI's line breaks
      replaced by spaces.
    - Counter-example: The one where a species has no English description, so the description is empty.

- **Rule: Must show the whole evolution chain, from the first stage to the last**

  | Pokémon viewed | Evolution chain shown |
    |---|---|
  | Ivysaur (middle stage) | Bulbasaur → Ivysaur → Venusaur |
  | Eevee (branching) | Eevee → Vaporeon, Jolteon, Flareon, … (each as a second stage) |
  | Tauros (does not evolve) | Tauros only |

- **Rule: Must reject an id that is not a positive whole number (400, with the standard error body)**

  | Id | Outcome |
    |---|---|
  | 0 | rejected |
  | -1 | rejected |
  | abc | rejected |

- **Rule: Must let anyone view details, signed in or not**
    - Example: The one where a visitor with no token views Bulbasaur.

- **Rule: Should reuse PokeAPI responses for 24 hours and keep serving them while PokeAPI is unavailable**
    - Example: The one where Bulbasaur is viewed twice within 24 hours, and PokeAPI is called only the first time.
    - Counter-example: The one where PokeAPI is down and the Pokémon is not cached, so the reader gets a 503 with the
      standard error body and no upstream details.
