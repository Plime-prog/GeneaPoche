# Branches à supprimer — liste établie le 15 novembre 2026

**Statut : non supprimées.** L'environnement de l'assistant refuse les écritures de suppression (HTTP 403) ; la suppression est à exécuter par le propriétaire du dépôt (voir ci-dessous). Nettoyage proposé : 186 branches. Seules figurent ici des branches **sans travail perdu** : entièrement contenues dans `main`, ou PR fusionnée avec le même commit de tête, ou ne portant que des fichiers de documentation (README/CHANGELOG, déjà publiés sous d'autres commits). Les **Releases GitHub et leurs APK ne sont pas touchées** : une release ne dépend pas de sa branche.

## Supprimer ces branches (propriétaire)

Depuis un clone local, avec la liste des noms de la dernière section (une branche par ligne dans un fichier `liste.txt`) :

```bash
xargs -n 40 git push origin --delete < liste.txt
```

Ou par l'interface GitHub : *Code → Branches*, icône corbeille. Réglage conseillé pour la suite : *Settings → General → « Automatically delete head branches »*.

## Restaurer une branche après suppression

```bash
git push origin <sha>:refs/heads/<nom-de-la-branche>
```

Le SHA est celui de la colonne « Tête » (utiliser le SHA complet). GitHub conserve les commits non référencés pendant un certain temps, sans garantie indéfinie.

## Branches conservées (14)

`main`, `release-0.111` (version courante), 8 branches de PR ouvertes (`changelog-v099-bis`, `fix-edition-prefill-v56`, 6 `dependabot/*`), et 5 branches portant des commits non fusionnés touchant du code ou le workflow, à examiner : `refonte-fiche-v33`, `release-0.32`, `release-0.33`, `release-0.81`, `release-0.95`.

## Branches à supprimer

| Branche | Tête | Raison |
|---|---|---|
| `actes-elsewhere-v83` | `f1038c1894d8a13ac1de238555fc74060b5c252e` | contenu entièrement dans main |
| `ajustements-fiches-v84` | `4422b21928ebfacb1a589512bd0962e695844462` | contenu entièrement dans main |
| `alertes-locales-v86` | `dab13e899e7f050363facf17d9d0d1974c891a77` | contenu entièrement dans main |
| `anomalies-v88` | `35cccdbc44f7b4c928ea2ff25efc95ff74414450` | contenu entièrement dans main |
| `arbre-complet-frise-v35` | `85bcd2333a0a9c2b81a4c6426ae6a2b8140c2aca` | contenu entièrement dans main |
| `audit-v108` | `a97222ea34d5a5918e70cd3bb8c54198b6deeaf2` | PR fusionnée |
| `audit-v31-perf` | `7fb2a287ee5e0e24ad8f48f1127098ea4e500977` | contenu entièrement dans main |
| `changelog-v084` | `14339746cdd1596abb1200fc460282a194532b14` | contenu entièrement dans main |
| `changelog-v085` | `e8f080a9755b996e8d62ff142f062805cf389f49` | PR fusionnée |
| `changelog-v086` | `727f4a16a78e3bcc5af5a1946527afe8a8fe9ef1` | PR fusionnée |
| `changelog-v088-communes` | `450d908016e12f2b83a263372d4928f1ae6ad9a1` | PR fusionnée |
| `changelog-v088-fix` | `6fa0afdc657ddf527485f3125b6f60c525f8c0b9` | PR fusionnée |
| `changelog-v089` | `2fc6e8f3d35b4b1b898ea0cca886ac534b47a7e9` | PR fusionnée |
| `changelog-v090` | `d1745d5ad09d83962865359066a01686d9b554a2` | PR fusionnée |
| `changelog-v091` | `a4b21884ff38ebdd972b5853b6fdfb9dccf9bbfc` | PR fusionnée |
| `changelog-v093` | `ba1236086f82e40ecaa874b8fac0d996e626e61b` | PR fusionnée |
| `changelog-v094` | `1e9b64318678e2581cc323f11c57b120636d91ed` | PR fusionnée |
| `changelog-v095` | `110b35db15e066858861c8ce118ba511a663b91c` | PR fusionnée |
| `changelog-v096` | `a99b5468f028a9e1d20d5e63ad37e16a0d8b8a71` | PR fusionnée |
| `changelog-v097` | `a629a464b1587cfa04fc35240d6bf642aceb6817` | PR fusionnée |
| `changelog-v098` | `bf5a7bcd7c235bae6c5a3b496c57da77df998296` | PR fusionnée |
| `changelog-v099` | `750b239cdd809fd0481357ca493ce48dfc508d32` | seulement des fichiers de doc |
| `changelog-v107` | `e5d89646b0c7da7ace16a82052d6e801ca911ce2` | PR fusionnée |
| `communes-v88` | `fe08074266683981d8b45d670b1c4943b8f02e1e` | contenu entièrement dans main |
| `communes2-v90` | `3fe3bcfbc2726d727ac9d0fafa33e79f95196a96` | contenu entièrement dans main |
| `communes3-v91` | `e7ace5c36502f4712850be118f21432528636bf3` | contenu entièrement dans main |
| `docs-dedupe-changelog` | `bce22e33e130dcf5afefff6cf4e0645005d18872` | contenu entièrement dans main |
| `docs-sync-v80` | `c161fadffccf77b623f45faa2d007f1a349c51ae` | contenu entièrement dans main |
| `docs-v033` | `8dfef592e0a592040b1b874851b9b499f893ae0b` | contenu entièrement dans main |
| `docs-v82` | `84dcd15f56b8217530e03d412c7f8ba6487703dc` | PR fusionnée |
| `docs-v83` | `943825ad9735565c0094852334af5192191c6d14` | contenu entièrement dans main |
| `export-chiffre-v85` | `798a32f773c0ea8cbffc53b6f3207e0a811a6d38` | contenu entièrement dans main |
| `fix-acte-v62` | `ef3793ae4c81b5ceadcf0109c4405f81a49d69b0` | contenu entièrement dans main |
| `fix-actes-nbmds-v67` | `4f4d3ee137d4040ada6725dc893635b5f34941e5` | contenu entièrement dans main |
| `fix-certif-conjoints-v89` | `15bbc212d4d62df9510577b97f0bbe21af6d0e3d` | contenu entièrement dans main |
| `fix-champs-parents-v65` | `1885c99abd52b26d58f00f07bdac587b31797696` | contenu entièrement dans main |
| `fix-compact-dates-v69` | `01a066cd9d98b097b704acb603c0f07efaf76737` | contenu entièrement dans main |
| `fix-conjoint-deces-parrains-v68` | `f1de527078064b369136a2915d9464e3998aae69` | contenu entièrement dans main |
| `fix-dates-zero-v71` | `a919908f25eca20d470c38acb15375393c17c549` | contenu entièrement dans main |
| `fix-doublons-chips-v61` | `7dd776508c32f72f267cd392005d66a36a9ab4b5` | contenu entièrement dans main |
| `fix-doublons-journal-v78` | `1aff1cbfbd6f70ff85d18a8a917ebc4277ec1457` | contenu entièrement dans main |
| `fix-enfant-avant-parent-v52` | `258cdd59564affd22af272094b4f1f4120d77e22` | contenu entièrement dans main |
| `fix-export-lock-v43` | `9e0a441adcedc9b61e442c4df7987f231067dca4` | contenu entièrement dans main |
| `fix-export-v41` | `0109ce8e1efe9749b0aad58b7bffa820a198619c` | contenu entièrement dans main |
| `fix-fiche-arbre-v39` | `e9a4931ee2ec727690a84927cf56be56482bf163` | contenu entièrement dans main |
| `fix-homonymes-v77` | `97b76d54c6f44549726f5a6b4567e2f863ab7410` | contenu entièrement dans main |
| `fix-import-v39` | `085a0c9bba793478a81f5c219e8f1934abb2ad2f` | contenu entièrement dans main |
| `fix-liens-parents-v59` | `b8fe537147abc8d1b29d4894d475d3ad1521788c` | contenu entièrement dans main |
| `fix-listes-compactes-v79` | `bb490194cccde0940834cedbd74942fc72f6db8a` | contenu entièrement dans main |
| `fix-officier-nmd-v74` | `2d74dd37fb740f2289daed28450e752e40e9d7d1` | contenu entièrement dans main |
| `fix-parrain-v76` | `cf4cb2fc2b7e201e80fd00038589535f2d07ca4d` | contenu entièrement dans main |
| `fix-personne-banniere-v63` | `3692d27fba52a151fdae331c99a5d9ec501ce5b7` | contenu entièrement dans main |
| `fix-prefill-dates-v0.57` | `3f28468d573f78d58302768ffd324277acb5bb2b` | contenu entièrement dans main |
| `fix-prefill-modification-v0.56` | `5c80f65e48992e0b5ee2a71fc2129d79683167c2` | contenu entièrement dans main |
| `fix-release-idempotente` | `cf33a1db726df44073f3048b71996e253eb6d604` | contenu entièrement dans main |
| `fix-release-workflow` | `45fa1a9be93303f48fcf5975f88add46118b59e8` | contenu entièrement dans main |
| `fix-rescue-bak-v42` | `e62f1244ba9a797b40e598f8389d59fea0507564` | contenu entièrement dans main |
| `fix-retouches-v72` | `f5c89b04f0872acef3384608e19d128e2c02da4b` | contenu entièrement dans main |
| `fix-retouches-v73` | `301e5913e7b6a9a7506ca76e40254dd0fe2df350` | contenu entièrement dans main |
| `fix-retour-liens-v64` | `9af96a51384ffa643bec8476af88e851a4bcc923` | contenu entièrement dans main |
| `fix-saisie-compacte-v58` | `8c62e1a166cef939042e70e5189146eb7485b2cb` | contenu entièrement dans main |
| `fix-saisie-compacte-v66` | `86226b73f9705410ace5e21c86e09b8e4a61aeeb` | contenu entièrement dans main |
| `fix-saisie-recherches-v49` | `748673ae9821b49d5cd9fad5967392154041e004` | contenu entièrement dans main |
| `fix-saisie-ultra-compacte-v70` | `7e84aa4f508ea848178aa0afd4fca97bac03a194` | contenu entièrement dans main |
| `fix-suspends-v80` | `21afa627c3100280fd5cc055518d11db985634c9` | contenu entièrement dans main |
| `fix-sync-total-v60` | `cddcb34db6612defe92c21c0a6e82e61d88b017d` | contenu entièrement dans main |
| `fix-urgences-v75` | `36205801273b5c3357d19572b767c3511e0a836f` | contenu entièrement dans main |
| `fonctionnalites-v86` | `a64af337e98c8ead9592e9167edf1120b981aed8` | contenu entièrement dans main |
| `fusion-v87` | `c12bc1e256e369a8eb1a12970ab503518b8dd3e4` | contenu entièrement dans main |
| `geneapoche10-coherence-v50` | `feac2587085d04c48151f3ec7b97c9e91bee304e` | contenu entièrement dans main |
| `geneapoche11-recherches-v51` | `f8f4b1ddd84cec5f80d254a0185032c9be6f5e92` | contenu entièrement dans main |
| `geneapoche12-diagnostic-v53` | `1c7940d0733b0265c1eedc3c7da1cd0e7e5aee81` | contenu entièrement dans main |
| `geneapoche12b-coches-tris-v54` | `0d588e65b01566022bb46542ebc7b1e8991cedcf` | contenu entièrement dans main |
| `geneapoche13-alertes-v55` | `52fd63ddbe821eea51471e58ba791db6f8c78978` | contenu entièrement dans main |
| `geneapoche2-fiche` | `6abbca15c48342c55186b050a42a91e7c0503786` | contenu entièrement dans main |
| `geneapoche3-v34` | `676d3c2d3b655408b4d9e613a2b0b83901b98cae` | contenu entièrement dans main |
| `geneapoche4-arbre-v37` | `45f301b378520ba72aca050c6ff5525662cdde5d` | contenu entièrement dans main |
| `geneapoche4-v36` | `b130e0715f24c34c210a7bc9967a9cfcd8c60537` | contenu entièrement dans main |
| `geneapoche5-v38` | `91af1650e1490f5492212ee80eecbbe5f01a8733` | contenu entièrement dans main |
| `geneapoche6-arbre-v44` | `890c9bf30a292057770dd36ab2e63f2d40465700` | contenu entièrement dans main |
| `geneapoche7-chronologie-v46` | `a55d6cdf95f903e7869b6fae91cf3b773c693543` | contenu entièrement dans main |
| `geneapoche8-lignedevie-v47` | `acf8e86968ef86cde53c9ab3b4290980d572fd63` | contenu entièrement dans main |
| `geneapoche9-synchronisation-v48` | `bd2c311787caf5888d0a856e0e1fffe507e17fff` | contenu entièrement dans main |
| `geo-v093` | `42ed65ed3ead16dc05d7b270ce28ecfe88e74cf7` | contenu entièrement dans main |
| `nom-apk-version-v45` | `ae503cc83b222fa500f859ac654c95c5825b3f09` | contenu entièrement dans main |
| `perf-deverrouillage-v81` | `a425338429bd20a99a5d0705d7a727a66f747549` | contenu entièrement dans main |
| `perf-v109` | `fed3f7447bc10c3f0607a9e8ee1a1c176634b3a1` | PR fusionnée |
| `readme-v72` | `607d9e25261034f8c2e875f30b881378cb4826fa` | contenu entièrement dans main |
| `release-0.100` | `511f01413c884f654899a8ec43c043f9a51f86b3` | contenu entièrement dans main |
| `release-0.101` | `30ea4879b4b8dcec204c289d4a765bb5f0513dc8` | contenu entièrement dans main |
| `release-0.102` | `9912db9dcb49a1606ed8b3b6a619b0f1e50ba329` | contenu entièrement dans main |
| `release-0.103` | `0bba50d539c53731327cc408ff5d115b0a5a6ea3` | contenu entièrement dans main |
| `release-0.104` | `6e9a35c4f04219ba8deab8dffdd9fb3c9d9a6794` | contenu entièrement dans main |
| `release-0.105` | `b6d9be20c3e9d5536ed748e5528560d4cf31e88f` | contenu entièrement dans main |
| `release-0.106` | `f9339480e4722873970027989a665f4c051a766c` | contenu entièrement dans main |
| `release-0.107` | `bd8952540c8558d6e414cbda19d29592aac105f2` | seulement des fichiers de doc |
| `release-0.108` | `e0788f82b9157ac204b8a4f0c843ce0e47a3f857` | contenu entièrement dans main |
| `release-0.109` | `87e13ab9b5d377daff7b497ec347064d46561736` | contenu entièrement dans main |
| `release-0.110` | `e5e3d3b22b2e15d8d5e12b71e017e9186bb69059` | contenu entièrement dans main |
| `release-0.34` | `d7463612afc1a712c369cc2c49590396f211581a` | seulement des fichiers de doc |
| `release-0.35` | `09905fb2ed42915adc8ceda318f70af707c872e2` | seulement des fichiers de doc |
| `release-0.36` | `04a84c8e2e4781eb883e4f189a0b1e4400d5c73f` | seulement des fichiers de doc |
| `release-0.37` | `7aa370bfec2ff4b17e5ddfd978bc9e1c433cbe30` | seulement des fichiers de doc |
| `release-0.38` | `fcd19cb0d38854cceaeb9e289106931ee9620cdf` | seulement des fichiers de doc |
| `release-0.39` | `fe49be95afe82b100020c205799558ff75387388` | seulement des fichiers de doc |
| `release-0.40` | `81ccddcc1dc7678004f1c91c89f0c0e898b22b19` | seulement des fichiers de doc |
| `release-0.41` | `6a00ad6a93c06ae89b6e199363262a1c3afe5342` | seulement des fichiers de doc |
| `release-0.42` | `ea0ff108088a1c7af0bd2f419fea6d78e581ccef` | seulement des fichiers de doc |
| `release-0.43` | `232f160d7a9fe567f33d9f138903748a13140816` | seulement des fichiers de doc |
| `release-0.44` | `f218d177c51cfc5b670973ff52600401df7f3e6e` | seulement des fichiers de doc |
| `release-0.45` | `e36a91a2766369d728dc916d57ecd2516b8de47f` | seulement des fichiers de doc |
| `release-0.46` | `6726e43c02f779ecbb7bfd09794826dd32b57cdd` | seulement des fichiers de doc |
| `release-0.47` | `9e748f075aada4e87b1463149369bfc26d1ff8de` | seulement des fichiers de doc |
| `release-0.48` | `93cda8b75041c44afa842bd9cb967f80fc8a5a74` | seulement des fichiers de doc |
| `release-0.49` | `374ad8e80128b40c1b22e49d92649eb747e5d16a` | seulement des fichiers de doc |
| `release-0.50` | `dc0e5014cdd2d67db72420a75ab931e167d4c19f` | seulement des fichiers de doc |
| `release-0.51` | `e8a73928f1e90b3b99705ca4ddd8b315f5b3a1e0` | seulement des fichiers de doc |
| `release-0.52` | `d3705bd1667adb0bce16a248f340ee2363d382e5` | seulement des fichiers de doc |
| `release-0.53` | `008152a6e353df701e9ebfcf639ebe6231733533` | seulement des fichiers de doc |
| `release-0.54` | `7a3ed483f426a6fbd2e63e0bb50056fd925b772c` | seulement des fichiers de doc |
| `release-0.55` | `559c4043b928fc01e5794bc92746ab07da9bc748` | seulement des fichiers de doc |
| `release-0.56` | `7f6f72374fc943bf8e2cacfd16d303430609969d` | seulement des fichiers de doc |
| `release-0.57` | `45298a1435a670313a9f5a409c1c0bf42aed5c2c` | seulement des fichiers de doc |
| `release-0.58` | `2235d18523c2a2ed3eed2d9aa573d5098927651f` | seulement des fichiers de doc |
| `release-0.59` | `5003e173778b2280ae6696bb8216f6d639ccb730` | seulement des fichiers de doc |
| `release-0.60` | `20b8dc42713496a06cc927ad4bf564b0e77a3622` | seulement des fichiers de doc |
| `release-0.61` | `cfb5087ac287630fee4b14004b93da9f19cf247d` | seulement des fichiers de doc |
| `release-0.62` | `2afbaf433479bfaa6834389e26a578e6d0fc6d50` | seulement des fichiers de doc |
| `release-0.63` | `397cad0f3679b909e2180dd90e42df5727266c9d` | seulement des fichiers de doc |
| `release-0.64` | `37c59fee1924e5077dff95ec9b14bc82fd4aee85` | seulement des fichiers de doc |
| `release-0.65` | `2926b42f878b8e9f3b460db091aecfb1237c370b` | seulement des fichiers de doc |
| `release-0.66` | `f7074fdaea56566ac2ea3352f08a30839221cd44` | seulement des fichiers de doc |
| `release-0.67` | `ca7ca875edcf6f0354efd9c118cc721e958fd02b` | seulement des fichiers de doc |
| `release-0.68` | `fdb5060deabc7f30429e6170ae423b4de0f11433` | seulement des fichiers de doc |
| `release-0.69` | `a0ffc5628e5cc311d4ad716ac899917f025c79e0` | seulement des fichiers de doc |
| `release-0.70` | `7ebfb1f0d5fadccb0774a0c00cd7d39c0d509ee1` | seulement des fichiers de doc |
| `release-0.71` | `3e084b42af5df3a54cdd74ac4dcb0dc5a649997d` | seulement des fichiers de doc |
| `release-0.72` | `a6db22667c2be16859e9454af4e85db1239a531f` | seulement des fichiers de doc |
| `release-0.73` | `7272c2ea733796737e5d62dbb32d0fa2bf9e86fa` | seulement des fichiers de doc |
| `release-0.74` | `14555b238e533952eeea245d8757827413c38219` | seulement des fichiers de doc |
| `release-0.75` | `33f73934b422ab05f37a70760459475c0c1d67e7` | seulement des fichiers de doc |
| `release-0.76` | `e314e64b2313243415fbf37101d7bede54e790a6` | seulement des fichiers de doc |
| `release-0.77` | `4e7124ebc205efa6e98e288c6e24df20cfb8dfa5` | seulement des fichiers de doc |
| `release-0.78` | `6efdb2ea5654c297a58a77f3426fa28fc59c20d7` | seulement des fichiers de doc |
| `release-0.79` | `dd948086c0b682374a81f13c5a9bb71b7d8d5dc3` | seulement des fichiers de doc |
| `release-0.80` | `b59f70a1b50a78f2ef512f51d7bda159401a6e9a` | seulement des fichiers de doc |
| `release-0.82` | `270496c5ab52ed74bb314bc4b9efcb3afa2daaea` | seulement des fichiers de doc |
| `release-0.83` | `d3f616cf9403fec67da1036977c5ca2330805788` | seulement des fichiers de doc |
| `release-0.84` | `df1f8d8d0bbd6f28d2f31c553804570703f52dd6` | seulement des fichiers de doc |
| `release-0.85` | `ccf0fbdff9d8d8d142ad47a0d0024fafcf5d652a` | seulement des fichiers de doc |
| `release-0.86` | `c014b8ffc06f4ad2b71eabe4b2514f815df54d2e` | seulement des fichiers de doc |
| `release-0.87` | `c12bc1e256e369a8eb1a12970ab503518b8dd3e4` | contenu entièrement dans main |
| `release-0.88` | `8623e1dc9f355749a32b7c8f33ba9c8bb319079c` | seulement des fichiers de doc |
| `release-0.89` | `4a41e50c7951ed4a432ff31a4f5fcd08f89cc5da` | seulement des fichiers de doc |
| `release-0.90` | `a05c9eddc1c60f6733b7d1c7faf03c64be5f2083` | seulement des fichiers de doc |
| `release-0.91` | `97b76ad95837cd6704dfb94ef9bc12a30bb074d8` | seulement des fichiers de doc |
| `release-0.92` | `a8b8d49bc54cd805904a4035b51faaefd8360f97` | contenu entièrement dans main |
| `release-0.93` | `f39c3d3010fd37ddf68f9fe88662f4bad7d0ef5d` | seulement des fichiers de doc |
| `release-0.94` | `7ec28768e3ec0c83c2b0628561516760a1aa2c8b` | seulement des fichiers de doc |
| `release-0.96` | `c34d276147471f16137f78fd4ec2555f0cc310fe` | seulement des fichiers de doc |
| `release-0.97` | `531b95fa12e347b995bbe20ace35a0a690a9cbb2` | seulement des fichiers de doc |
| `release-0.98` | `b0b43d1d57a6ed52862249e2107c3228454ff50d` | seulement des fichiers de doc |
| `release-0.99` | `bc3e79f19faec405333231f013c71e84c9978274` | contenu entièrement dans main |
| `reorg-v094` | `a02fb8473753178b0525c6264cb92f6eb398ccd9` | contenu entièrement dans main |
| `reorg-v095` | `175b341ae5a282ac8aa6060d1e04d6e2b71db05a` | contenu entièrement dans main |
| `sauvegarde-auto-v82` | `a9cdfaf1a87b78dc7e20de8b89b482df35c9be0b` | contenu entièrement dans main |
| `sources-v89` | `20a27b5571b3797a7dca657b5420d4921f85d4c3` | contenu entièrement dans main |
| `split-v111` | `38bc009617b588ffe20fab2b93c1459610e28b37` | PR fusionnée |
| `tests-v110` | `3d37c9fed6104721e120384825ec819e49ea7d96` | PR fusionnée |
| `v0.32-release` | `110269e7d6bc0f15d536c6d6730203884de3da95` | contenu entièrement dans main |
| `v0100-recherches` | `d740a6eb191e1c20cb3775c6bc1c82f3fb6e5d8b` | contenu entièrement dans main |
| `v0101-saisie` | `39c1d26b333739a19a7b8d78252b40cb9feb00c5` | contenu entièrement dans main |
| `v0102-biographie` | `e20087a9b28537dfd9130fc6909613c7d11c8adc` | contenu entièrement dans main |
| `v0103-fiche` | `6fdfe056b329206bb7a3d94fcb64efc805cc7dae` | contenu entièrement dans main |
| `v0104-stabilite` | `8e0dbcf40464bc2846f56966c54c18a2aae239b2` | contenu entièrement dans main |
| `v0105-corrections` | `c021c2e680482df589324b6ff82de3835e14403a` | contenu entièrement dans main |
| `v0106-mariage` | `68a885e66d722510f32900ed9cb7df79705364e2` | contenu entièrement dans main |
| `v096` | `aa6ad97ac6c60c3c49ddb655565ebaaa59eade6c` | contenu entièrement dans main |
| `v097` | `e7e0ecb66dbbe58e7d2747293b0191532c353c7a` | contenu entièrement dans main |
| `v098` | `0ebacf04ac8d0044f503a90c9069bc38e653983c` | contenu entièrement dans main |
| `v099` | `b30d45690cd105dc68916ec46cb57965cf5b91a3` | contenu entièrement dans main |
| `v099-finitions` | `e4d0d9c6cea0aa1d6651fd4c22fb52d4047a66cd` | contenu entièrement dans main |
| `v107` | `31c4d886936d882b5557842799b4f437f093e30c` | contenu entièrement dans main |
| `v107-tmp-init` | `f9339480e4722873970027989a665f4c051a766c` | contenu entièrement dans main |
| `v40-fiche-arbre` | `fa023acd7fc5ecf35c0420213d9ce442b492ed1c` | contenu entièrement dans main |
| `zero-internet` | `7d5687cf449d5edd944525ee0591ef878f61f166` | contenu entièrement dans main |

## Ajout du 17 novembre 2026 : branches de travail v0.112 à v0.131 (toutes fusionnées)

Branches créées depuis la première liste. Les branches de travail (`*-v1NN`) sont fusionnées en squash dans `main` ; les `release-0.NNN` ont servi à déclencher la publication (la release GitHub et ses APK restent après suppression de la branche). **Conserver `release-0.131` si vous voulez pouvoir republier ; ne pas recréer une branche `release-*` supprimée sans le vouloir : cela relancerait une publication.**

```
acte-v117
actions-v113
actions-v119
agp-v121
agp-v122
cujus-v112
decoupe-v123
deps-v118
doc-v099
events-v130
famille-v131
gedcom-v127
gradle-ci-v121
gradle-v120
import-v114
import-v128
import-v129
lint-v125
noms-v124
pin-v126
release-0.112
release-0.113
release-0.114
release-0.115
release-0.116
release-0.117
release-0.118
release-0.119
release-0.120
release-0.121
release-0.122
release-0.123
release-0.124
release-0.125
release-0.126
release-0.127
release-0.128
release-0.129
release-0.130
release-0.131
remplacer-v115
restauration-v116
```

## Ajout du 17 novembre (v0.134 et v0.135)

Branches de travail déjà fusionnées dans `main`, à supprimer : `notes-v134`, `sources-v135`, `bilan-maj`, `branches-maj`.

Branches de déclenchement des releases (les fichiers restent dans la page Releases) : `release-0.132`, `release-0.133`, `release-0.134`, `release-0.135`.

À conserver : `main`.

## Ajout du 18 novembre (v0.136 à v0.138)

Branches de travail fusionnées dans `main`, à supprimer : `taille-v136`, `retour-v136`, `retours-v137`, `docs-v138`, et les branches de la v0.137 « export GEDCOM » créées en parallèle (la pull request `#160` est restée ouverte alors que `#159` est fusionnée : à fermer).

Branches de déclenchement de release : `release-0.136`, `release-0.137`, `release-0.138`. À conserver : `main`.
