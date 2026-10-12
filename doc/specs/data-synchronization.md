# US-03 Data Synchronization

Copies a Pokémon from PokeAPI into the local database, so it can carry our own fields on top of PokeAPI's.
The local copy holds: id, name, category, weight, sprite (from PokeAPI) and localized name, region and tags (our own
fields, empty until edited in US-04).

- **Rule: Must copy a Pokémon from PokeAPI into the local database, with our own fields empty**
    - Example: The one where Bulbasaur (id 1) is synchronised, so the local copy has id 1, name "bulbasaur",
      category "Seed Pokémon", weight 69, its sprite, no localized name, no region and no tags (201).
    - Counter-example: The one where PokeAPI has no Pokémon with that id, so nothing is stored and the reader gets a
      404 with the standard error body.

- **Rule: Must refresh PokeAPI's fields when a stored Pokémon is synchronised again, keeping our own fields**
    - Example: The one where Bulbasaur already has localized name "Bisasam" and region "Kanto"; synchronising it again
      updates the PokeAPI fields and keeps "Bisasam" and "Kanto" (200).

- **Rule: Must list stored Pokémon one page at a time, ordered by id**
    - Example: The one where 3 Pokémon are stored and page 0 of size 20 returns all 3, with the total and total pages.
    - Counter-example: The one where nothing is stored, so the page is empty with a total of 0.

- **Rule: Must show one stored Pokémon by its id**
    - Example: The one where stored Bulbasaur is returned with both PokeAPI's fields and our own.
    - Counter-example: The one where the id is not stored locally, so the reader gets a 404 with the standard error
      body, even if PokeAPI knows the Pokémon.

- **Rule: Must let only signed-in users synchronise; anyone can read the local list and details**

  | Action | No token | Valid token |
    |---|---|---|
  | synchronise | 401 | allowed |
  | list / view stored | allowed | allowed |

- **Rule: Must come pre-populated with demo data**
    - Example: The one where the app starts with an empty database and the first 10 Pokémon (Bulbasaur … Caterpie)
      are already stored, without calling PokeAPI.
    - Counter-example: The one where the app restarts with data already stored, so nothing is overwritten.
