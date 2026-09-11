package com.senze.miaokaka.model.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.senze.miaokaka.model.entity.CoinTransaction;
import lombok.Data;

import java.io.Serializable;

/**
 * 喵币钱包视图：余额 + 流水分页
 *
 * @author <a href="https://github.com/eyeskeeper">冉森</a>
 */
@Data
public class WalletVO implements Serializable {

    private Integer balance;

    private Page<CoinTransaction> transactions;

    private static final long serialVersionUID = 1L;
}
