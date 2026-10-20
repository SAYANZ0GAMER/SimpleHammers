# Simple Hammers (Java Edition / Paper)

Port of the Bedrock add-on "Simple Hammers": six hammers that mine a 3x3x1 area.

Requires: Paper (or a fork) 1.21.4+ and Java 21.

## Build
    mvn package
The jar is created in `target/SimpleHammers-2.2.0.jar`. Drop it in `plugins/`.

## Crafting
Stone / Copper / Iron / Golden / Diamond hammer: 
    X X X
    X S X      X = material (cobblestone, copper, iron, gold, diamond), S = stick
      S
Netherite hammer: smithing table = Diamond hammer + Netherite ingot + Netherite upgrade template.

## Commands
`/hammer give <type> [player] [amount]`, `/hammer reload`  (permission `simplehammers.admin`)

## Look
No resource pack needed: each hammer looks exactly like the vanilla pickaxe of the same tier, with its own name (e.g. "Diamond Hammer") and a "Breaks a 3x3x1 area" tooltip.
