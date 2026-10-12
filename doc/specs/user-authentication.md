# US-05 User Authentication

Users register and sign in to get a token. The token unlocks the protected actions (synchronise, update,
delete); browsing and viewing stay public.

- **Rule: Must register a new user with a username and password**
    - Example: The one where "ash" registers with password "pikachu123" and gets 201, without the password in the
      response.
    - Counter-example: The one where "ash" is already taken, so the reader gets a 409 with the standard error body.

- **Rule: Must reject registrations with invalid values (400, with the standard error body)**

  | Username | Password | Outcome |
    |---|---|---|
  | ash | pikachu123 | accepted |
  | as | pikachu123 | rejected (username under 3 characters) |
  | *(31 characters)* | pikachu123 | rejected (username over 30 characters) |
  | ash | short | rejected (password under 8 characters) |

- **Rule: Must never store passwords in readable form**
    - Example: The one where the stored password for "ash" is a hash, not "pikachu123".

- **Rule: Must give a token valid for 1 hour to a user who signs in with the right credentials**
    - Example: The one where "ash" signs in with "pikachu123" and gets a token (200).
    - Counter-example: The one where the password is wrong or the user does not exist; both get the same 401, so the
      reader cannot tell which.

- **Rule: Must accept only valid, unexpired tokens on protected actions**

  | Token | Outcome |
    |---|---|
  | none | 401 |
  | tampered or malformed | 401 |
  | expired (over 1 hour old) | 401 |
  | valid | allowed |

- **Rule: Must come with a demo user**
    - Example: The one where the app starts and "demo" / "demo1234" can sign in straight away.
