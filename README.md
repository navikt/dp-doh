# Dagpenger-SERVICENAME

## Komme i gang

Gradle brukes som byggverktøy og er bundlet inn.

`./gradlew build`

## Jenkins

Alle repos som begynner på `dagpenger` eller `dp` blir automatisk fanget opp av
Jenkins. Den sjekker om det finnes en Jenkinsfile og lager automatisk en jobb.

[Jenkinsfile](Jenkinsfile) vil automatisk bygge med gradle. Lykkes bygget vil
den automatisk deploye til preprod og kjøre end-to-end tester. De testene settes
opp med å legge til en `scripts`-katalog med script inni.

Lykkes det vil bygget automatisk deployes til produksjon så lenge man er på
master-branch.

---

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan rettes mot:

* André Roaldseth, andre.roaldseth@nav.no
* Eller en annen måte for omverden å kontakte teamet på

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #dagpenger.
