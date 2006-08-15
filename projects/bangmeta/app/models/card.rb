class Card < ActiveRecord::Base
  def self.find_all_for_town (town)
    find(:all, :conditions => [ "town = ?", town ])
  end

  def done?
    self.implemented? &&
      (bonus_model == "" || bonus_model_path != "") &&
      (activation_viz == "" || activation_viz_path != "") &&
      (activation_sound == "" || activation_sound_path != "") &&
      (ongoing_viz == "" || ongoing_viz_path != "") &&
      (special_sound == "" || special_sound_path != "");
  end

  def icon_link
    ident == "" ? "" :
      "<img src=\"#{SVNURL}/cards/#{town}/#{ident}/icon.png\">";
  end

  def styled_name
    done? ? name : "<em>#{name}</em>"
  end

  def styled_effect
    implemented? ? effect : "<em>#{effect}</em>"
  end

  def bonus_model_link
    if (bonus_model_path == "bonuses" ||
        bonus_model_path == "extras")
      path = "#{bonus_model_path}/#{town}/#{ident}"
    else
      path = bonus_model_path
    end
    path = "#{path}/model.properties" unless (path == "")
    link(bonus_model == "" || bonus_model_path != "", bonus_model, path)
  end

  def activation_viz_link (icon_mode = false)
    link(activation_viz == "" || activation_viz_path != "", 
         activation_viz, effect_path(activation_viz_path))
  end

  def activation_sound_link (icon_mode = false)
    link(activation_sound == "" || activation_sound_path != "", 
         activation_sound, activation_sound_path)
  end

  def ongoing_viz_link (icon_mode = false)
    link(ongoing_viz == "" || ongoing_viz_path != "", 
         ongoing_viz, effect_path(ongoing_viz_path))
  end

  def special_sound_link (icon_mode = false)
    link(special_sound == "" || special_sound_path != "", 
         special_sound, special_sound_path)
  end

  def activation_viz_icon (icon_mode = false)
    icon(activation_viz, effect_path(activation_viz_path), "effect")
  end

  def activation_sound_icon (icon_mode = false)
    icon(activation_sound, activation_sound_path, "sound")
  end

  def ongoing_viz_icon (icon_mode = false)
    icon(ongoing_viz, effect_path(ongoing_viz_path), "effect")
  end

  def special_sound_icon (icon_mode = false)
    icon(special_sound, special_sound_path, "sound")
  end

  def effect_path (path)
    if (path == "none" || path == "")
      path
    elsif (path =~ /\//)
      "#{path}/effect.properties"
    else
      "effects/#{town}/#{path}/effect.properties"
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
      return "<img src=\"/images/#{type}_icon.png\">"
    elsif (path == "")
      return "<img src=\"/images/missing_icon.png\">"
    else
      return "<a href=\"#{SVNURL}/#{path}\">" +
        "<img border=0 src=\"/images/#{type}_icon.png\"></a>"
    end
  end

  SVNURL = "https://src.earth.threerings.net/viewvc/bang/trunk/rsrc"
end
