package com.casinocore.integrations.citizens;

import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;

@TraitName("casinogame")
public class CasinoGameTrait extends Trait {

    @Persist("game")
    private String gameName;

    public CasinoGameTrait() {
        super("casinogame");
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }
}
