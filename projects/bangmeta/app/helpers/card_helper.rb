module CardHelper
  def icon_link (card)
    card.ident == "" ? "" :
      "<img src=\"#{SVNURL}/cards/#{card.town}/#{card.ident}/icon.png\">";
  end

  def styled_name (card)
    card.done? ? card.name : "<em>#{card.name}</em>"
  end

  def styled_effect (card)
    card.implemented? ? card.effect : "<em>#{card.effect}</em>"
  end

  def bonus_model_link (card)
    if (card.bonus_model_path == "bonuses" ||
        card.bonus_model_path == "extras")
      path = "#{card.bonus_model_path}/#{card.town}/#{card.ident}"
    else
      path = card.bonus_model_path
    end
    path = "#{path}/model.properties" unless (path == "")
    link(card.bonus_model == "" || card.bonus_model_path != "", 
         card.bonus_model, path)
  end

  def viz_link (card, viz, viz_path)
    link(viz == "" || viz_path != "", viz, effect_path(card, viz_path))
  end

  def sound_link (card, sound, sound_path)
    link(sound == "" || sound_path != "", sound, sound_path)
  end

  def viz_icon (card, viz, viz_path)
    icon(viz, effect_path(card, viz_path), "effect")
  end

  def sound_icon (card, sound, sound_path)
    icon(sound, sound_path, "sound")
  end

  def effect_path (card, path)
    if (path == "none" || path == "")
      path
    elsif (path =~ /\//)
      "#{path}/effect.properties"
    else
      "effects/#{card.town}/#{path}/effect.properties"
    end
  end

  def link (condition, text, path)
    if (condition)
      (path == "none") ? text : "<a href=\"#{SVNURL}/#{path}\">#{text}</a>"
    else 
      "<em>#{text}</em>"
    end
  end

  def icon (resource, path, type)
    if (resource == "")
      return ""
    elsif (path == "none")
      return image_tag("#{type}_icon")
    elsif (path == "")
      return image_tag("missing_icon")
    else
      return "<a href=\"#{SVNURL}/#{path}\">" +
        image_tag("#{type}_icon", :border => "0") + "</a>"
    end
  end

  SVNURL = "https://src.earth.threerings.net/viewvc/bang/trunk/rsrc"
end
