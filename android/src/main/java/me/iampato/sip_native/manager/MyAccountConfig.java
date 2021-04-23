package me.iampato.sip_native.manager;

import org.pjsip.pjsua2.*;

import java.util.ArrayList;

public class MyAccountConfig {
    public AccountConfig accCfg = new AccountConfig();
    public ArrayList<BuddyConfig> buddyCfgs = new ArrayList<BuddyConfig>();

    public void readObject(ContainerNode node)
    {
        try
        {
            ContainerNode acc_node = node.readContainer("Account");
            accCfg.readObject(acc_node);
            ContainerNode buddies_node = acc_node.readArray("buddies");
            buddyCfgs.clear();
            while (buddies_node.hasUnread())
            {
                BuddyConfig bud_cfg = new BuddyConfig();
                bud_cfg.readObject(buddies_node);
                buddyCfgs.add(bud_cfg);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void writeObject(ContainerNode node)
    {
        try
        {
            ContainerNode acc_node = node.writeNewContainer("Account");
            accCfg.writeObject(acc_node);
            ContainerNode buddies_node = acc_node.writeNewArray("buddies");
            for (int j = 0; j < buddyCfgs.size(); j++)
            {
                buddyCfgs.get(j).writeObject(buddies_node);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
