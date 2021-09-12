package de.siphalor.nmuk.impl;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

@Environment(EnvType.CLIENT)
public class ErrorMessageToast implements Toast {
	private static final long SHOW_TIME = 2000L;
	private static final int MIN_WIDTH = 200;
	private final ErrorMessageToast.Type type;
	private Text title;
	private List<OrderedText> lines;
	private long startTime;
	private boolean justUpdated;
	private int width;

	public ErrorMessageToast(ErrorMessageToast.Type type) {
		this.type = type;
		title = type.title;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public Toast.Visibility draw(MatrixStack matrices, ToastManager manager, long startTime) {
		if (justUpdated) {
			this.startTime = startTime;
			justUpdated = false;
		}

		RenderSystem.setShaderTexture(0, TEXTURE);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		int i = getWidth();
		if (i == 160 && lines.size() <= 1) {
			manager.drawTexture(matrices, 0, 0, 0, 64, i, getHeight());
		} else {
			int o = getHeight() + Math.max(0, lines.size() - 1) * 12;
			int m = Math.min(4, o - 28);
			drawPart(matrices, manager, i, 0, 0, 28);

			for (int n = 28; n < o - m; n += 10) {
				drawPart(matrices, manager, i, 16, n, Math.min(16, o - n - m));
			}

			drawPart(matrices, manager, i, 32 - m, o - m, m);
		}

		if (lines == null) {
			manager.getGame().textRenderer.draw(matrices, title, 18.0F, 12.0F, -256);
		} else {
			manager.getGame().textRenderer.draw(matrices, title, 18.0F, 7.0F, -256);

			for (int o = 0; o < lines.size(); ++o) {
				manager.getGame().textRenderer.draw(matrices, lines.get(o), 18.0F, 18 + o * 12, -1);
			}
		}

		return startTime - this.startTime < SHOW_TIME ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
	}

	private void drawPart(MatrixStack matrices, ToastManager manager, int width, int textureV, int y, int height) {
		int i = textureV == 0 ? 20 : 5;
		int j = Math.min(60, width - i);
		manager.drawTexture(matrices, 0, y, 0, 64 + textureV, i, height);

		for (int k = i; k < width - j; k += 64) {
			manager.drawTexture(matrices, k, y, 32, 64 + textureV, Math.min(64, width - k - j), height);
		}

		manager.drawTexture(matrices, width - j, y, 160 - j, 64 + textureV, j, height);
	}

	public void updateContent(MinecraftClient client, Object... args) {
		TextRenderer textRenderer = client.textRenderer;
		List<OrderedText> list = textRenderer.wrapLines(type.constructDescriptionText(args), MIN_WIDTH);
		int requiredWidth = Math.max(MIN_WIDTH, list.stream().mapToInt(textRenderer::getWidth).max().orElse(MIN_WIDTH));

		lines = list;
		width = requiredWidth + 30;
		justUpdated = true;
	}

	@Override
	public ErrorMessageToast.Type getType() {
		return type;
	}

	public static void add(ToastManager manager, ErrorMessageToast.Type type, Object... args) {
		ErrorMessageToast toast = new ErrorMessageToast(type);
		toast.updateContent(manager.getGame(), args);
		manager.add(toast);
	}

	public static void show(ToastManager manager, ErrorMessageToast.Type type, Object... args) {
		ErrorMessageToast toast = manager.getToast(ErrorMessageToast.class, type);
		if (toast == null) {
			add(manager, type, args);
		} else {
			toast.updateContent(manager.getGame(), args);
		}
	}

	private static final Text BIND_EXISTING_KEYS_FIRST = new TranslatableText("nmuk.options.controls.bind_existing_keys_first");

	@Environment(EnvType.CLIENT)
	public static enum Type {
		MAIN_KEY_UNBOUND(BIND_EXISTING_KEYS_FIRST, "nmuk.options.controls.main_key_unbound"),
		CHILDREN_KEY_UNBOUND_TRANSLATION_KEY(BIND_EXISTING_KEYS_FIRST, "nmuk.options.controls.children_key_unbound");

		public final Text title;
		public final String descriptionTranslationKey;

		private Type(Text title, String descriptionTranslationKey) {
			this.title = title;
			this.descriptionTranslationKey = descriptionTranslationKey;
		}

		public Text constructDescriptionText(Object... args) {
			if (descriptionTranslationKey == null) {
				String text = "";
				if (args.length > 0) {
					text = args[0].toString();
				}
				return new LiteralText(text);
			}
			return new TranslatableText(descriptionTranslationKey, args);
		}
	}
}
