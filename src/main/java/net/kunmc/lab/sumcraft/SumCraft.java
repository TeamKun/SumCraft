package net.kunmc.lab.sumcraft;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class SumCraft extends JavaPlugin implements Listener {
    // プレイヤーのメモ
    private static class PlayerData {
        // 最後にのったブロックをメモっておく
        public Block lastPosition;
    }

    // プレイヤーのIDとメモを紐付けておく
    private Map<UUID, PlayerData> playerData = new HashMap<>();
    // ブロックの対応表
    private Map<Material, Integer> blockMap = new HashMap<>();
    // スタート地点
    private Location startLocation;
    // 今の数字スコアボード
    private Objective sumObjective;
    // 合わせる数字スコアボード
    private Objective goalSumObjective;

    @Override
    public void onEnable() {
        // イベントを登録
        getServer().getPluginManager().registerEvents(this, this);

        // デフォルトコンフィグを作る
        saveDefaultConfig();

        // コンフィグからスタート地点を読み込む
        startLocation = getConfig().getLocation("location.start");

        // スコアボードのsumスコアを作る
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        // 今の数字
        sumObjective = scoreboard.getObjective("sum");
        // sumスコアなかったらつくる
        if (sumObjective == null) {
            sumObjective = scoreboard.registerNewObjective("sum", "dummy", "数字");
        }

        // 目標の数字
        goalSumObjective = scoreboard.getObjective("goal_sum");
        // sumスコアなかったらつくる
        if (goalSumObjective == null) {
            goalSumObjective = scoreboard.registerNewObjective("goal_sum", "dummy", "目標");
        }

        // コンフィグに書いたブロックを読み込みます
        for (int i = -1; i < 10; ++i) {
            // IDをコンフィグから読む
            String id = getConfig().getString("block." + i);
            if (id == null) {
                continue;
            }
            // IDからブロックの素材に変換
            Material material = Material.getMaterial(id);
            if (material == null) {
                continue;
            }
            // 記憶
            blockMap.put(material, i);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // プレイヤーを取得
        Player player = event.getPlayer();
        // プレイヤーデータをIDから取得
        UUID id = player.getUniqueId();
        // プレイヤーのIDからメモを取得、なかったらつくります
        PlayerData data = playerData.computeIfAbsent(id, i -> new PlayerData());
        // 足元のブロックを取得 (足-1)
        Block foot = player.getLocation().getBlock().getRelative(BlockFace.DOWN);

        // 足元のブロックが変わってなかったら弾く
        if (data.lastPosition != null && Objects.equals(foot.getLocation(), data.lastPosition.getLocation())) {
            return;
        }

        // 足元のブロックのメモを残す
        data.lastPosition = foot;

        // 数字を取り出す
        Integer num = blockMap.get(foot.getType());
        // 数字ブロックじゃないときは弾く
        if (num == null) {
            return;
        }

        // スコアを取得
        Score sumScore = sumObjective.getScore(player.getName());
        int oldScore = sumScore.getScore();

        // 目標のスコアを取得
        Score goalSumScore = goalSumObjective.getScore(player.getName());
        // 目標のスコア
        int goalSum = goalSumScore.getScore();

        // ゴールブロックを踏んだら
        if (num == -1 && startLocation != null) {
            // 目標と違ったらスタート送り
            if (goalSum != oldScore) {
                // スタートに行く
                player.teleport(startLocation);
                // 不正解
                player.sendActionBar("不正解！ 合計スコアを" + goalSum + "にしてきてね");
                // 音
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 1);
            }
        } else {
            // スコアに数字を足して、10で割ったあまりでスコアを更新
            int newScore = (oldScore + num) % 10;
            sumScore.setScore(newScore);

            // タイトル
            player.sendTitle("", String.format((goalSum == newScore ? ChatColor.GREEN : ChatColor.GRAY) + "%d + %d = %d [%d]", oldScore, num, newScore, goalSum), 0, 10000, 0);

            // 音
            player.playSound(player.getLocation(), Sound.BLOCK_BAMBOO_BREAK, 1, 1);

            // デバッグのチャット
            //player.sendMessage("" + oldScore);
        }
    }

}
