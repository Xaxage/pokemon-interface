# US-04 Local Data Modification

Signed-in users edit or delete Pokémon stored in the local database (US-03). Only our own fields can be
edited: PokeAPI's fields (id, name, category, weight, sprite) are owned by synchronisation.

- **Rule: Must replace our own fields of a stored Pokémon with the ones sent**
    - Example: The one where Bulbasaur is updated with localized name "Bisasam", region "Kanto" and tags
      ["starter", "grass"], and comes back with those values (200).
    - Example: The one where a field is left out of the update, so it is cleared (a full replace, not a merge).
    - Counter-example: The one where the update also sends a different weight, so the weight is ignored and stays 69.

- **Rule: Must reject updates with invalid values (400, with the standard error body listing each invalid field)**

  | Field | Valid | Rejected |
    |---|---|---|
  | localized name | absent, or 1–50 characters | blank, or over 50 characters |
  | region | absent, or 1–50 characters | blank, or over 50 characters |
  | tags | absent, or up to 10 tags | more than 10 tags |
  | each tag | 1–30 characters | blank, over 30 characters, or a duplicate |

    - Counter-example: The one where the body is not valid JSON, so the reader gets a 400 and nothing is changed.

- **Rule: Must delete a stored Pokémon**
    - Example: The one where Bulbasaur is deleted (204), and viewing it afterwards gives a 404.

- **Rule: Must answer 404 when the Pokémon is not stored locally**

  | Action | Id not stored | Outcome |
    |---|---|---|
  | update | 99999 | 404, nothing stored |
  | delete | 99999 | 404 |

- **Rule: Must let only signed-in users update or delete**
    - Example: The one where an update with no token gets a 401 and nothing is changed.
    - Counter-example: The one where the token has expired, so the update gets a 401 as well.
